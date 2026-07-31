package com.ones.admin.auth.oauth;

import com.ones.admin.auth.AuthErrorCode;
import com.ones.admin.auth.oauth.model.ExternalIdentity;
import com.ones.admin.auth.oauth.repository.ExternalIdentityRepository;
import com.ones.admin.auth.repository.UserRepository;
import com.ones.admin.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ExternalIdentityService {

    private final ExternalIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final ThirdPartyAuthProviderRegistry providerRegistry;

    public ExternalIdentityService(
            ExternalIdentityRepository identityRepository,
            UserRepository userRepository,
            ThirdPartyAuthProviderRegistry providerRegistry
    ) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.providerRegistry = providerRegistry;
    }

    public List<ExternalIdentityResponse> list(Long userId) {
        requireUser(userId);
        return identityRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    public ExternalIdentityResponse bind(Long userId, ExternalIdentityBindRequest request) {
        requireUser(userId);
        String provider = request.provider().trim().toLowerCase(Locale.ROOT);
        requireKnownProvider(provider);
        String tenantKey = request.tenantKey().trim();
        String externalSubject = request.externalSubject().trim();
        ExternalIdentity existing = identityRepository.findAny(provider, tenantKey, externalSubject).orElse(null);
        if (existing != null && !existing.userId().equals(userId)) {
            throw new BusinessException(AuthErrorCode.EXTERNAL_IDENTITY_CONFLICT);
        }
        ExternalIdentity identity = existing == null
                ? new ExternalIdentity(null, provider, tenantKey, externalSubject, userId,
                        ExternalIdentityStatus.ACTIVE, null)
                : new ExternalIdentity(existing.id(), provider, tenantKey, externalSubject, userId,
                        ExternalIdentityStatus.ACTIVE, existing.lastLoginAt());
        try {
            return toResponse(identityRepository.save(identity));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(AuthErrorCode.EXTERNAL_IDENTITY_CONFLICT);
        }
    }

    public void unbind(Long userId, Long identityId) {
        requireUser(userId);
        identityRepository.disable(identityId, userId, LocalDateTime.now());
    }

    private void requireUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_AVAILABLE));
    }

    private void requireKnownProvider(String provider) {
        boolean known = providerRegistry.all().stream().anyMatch(candidate -> candidate.id().equals(provider));
        if (!known) {
            throw new BusinessException(AuthErrorCode.OAUTH_PROVIDER_NOT_ENABLED);
        }
    }

    private ExternalIdentityResponse toResponse(ExternalIdentity identity) {
        return new ExternalIdentityResponse(
                identity.id(), identity.provider(), identity.tenantKey(), identity.externalSubject(),
                identity.userId(), identity.status(), identity.lastLoginAt()
        );
    }
}
