package com.ones.admin.auth.oauth;

import com.ones.admin.auth.AuthErrorCode;
import com.ones.admin.auth.AuthService;
import com.ones.admin.auth.dto.LoginResponse;
import com.ones.admin.auth.oauth.model.ExternalIdentity;
import com.ones.admin.auth.oauth.model.ExternalIdentityProfile;
import com.ones.admin.auth.oauth.repository.ExternalIdentityRepository;
import com.ones.admin.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OAuthFlowService {

    private final ThirdPartyAuthProviderRegistry providerRegistry;
    private final OAuthFlowStore flowStore;
    private final ExternalIdentityRepository identityRepository;
    private final AuthService authService;
    private final OAuthTokenGenerator tokenGenerator;
    private final OAuthFlowProperties properties;

    public OAuthFlowService(
            ThirdPartyAuthProviderRegistry providerRegistry,
            OAuthFlowStore flowStore,
            ExternalIdentityRepository identityRepository,
            AuthService authService,
            OAuthTokenGenerator tokenGenerator,
            OAuthFlowProperties properties
    ) {
        this.providerRegistry = providerRegistry;
        this.flowStore = flowStore;
        this.identityRepository = identityRepository;
        this.authService = authService;
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
    }

    public List<AuthProviderCapability> providers() {
        return providerRegistry.all().stream()
                .map(provider -> new AuthProviderCapability(provider.id(), provider.displayName(), provider.enabled()))
                .toList();
    }

    public OAuthAuthorizeResult authorize(String providerId) {
        ThirdPartyAuthProvider provider = providerRegistry.requireEnabled(providerId);
        String state = tokenGenerator.generate();
        flowStore.saveState(provider.id(), state, properties.getStateTtl());
        return new OAuthAuthorizeResult(
                provider.authorizationUrl(state),
                properties.getStateTtl().toSeconds()
        );
    }

    public OAuthCallbackResult callback(String providerId, String authorizationCode, String state) {
        ThirdPartyAuthProvider provider = providerRegistry.requireEnabled(providerId);
        if (!flowStore.consumeState(provider.id(), state)) {
            throw new BusinessException(AuthErrorCode.OAUTH_STATE_INVALID);
        }
        ExternalIdentityProfile profile = provider.authenticate(authorizationCode);
        if (!provider.id().equals(profile.provider())) {
            throw new BusinessException(AuthErrorCode.OAUTH_PROVIDER_FAILED);
        }
        ExternalIdentity identity = identityRepository.findActive(
                        profile.provider(),
                        profile.tenantKey(),
                        profile.externalSubject()
                )
                .orElseThrow(() -> new BusinessException(AuthErrorCode.EXTERNAL_IDENTITY_NOT_BOUND));
        String ticket = tokenGenerator.generate();
        flowStore.saveTicket(ticket, new OAuthLoginTicket(
                identity.userId(),
                identity.id(),
                identity.provider()
        ), properties.getTicketTtl());
        return new OAuthCallbackResult(ticket, properties.getTicketTtl().toSeconds());
    }

    public OAuthExchangeResult exchange(String ticket) {
        OAuthLoginTicket loginTicket = flowStore.consumeTicket(ticket)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.OAUTH_TICKET_INVALID));
        LoginResponse loginResponse = authService.loginByUserId(loginTicket.userId());
        identityRepository.recordLogin(loginTicket.externalIdentityId(), LocalDateTime.now());
        return new OAuthExchangeResult(
                loginResponse,
                loginTicket.provider(),
                loginTicket.externalIdentityId()
        );
    }
}
