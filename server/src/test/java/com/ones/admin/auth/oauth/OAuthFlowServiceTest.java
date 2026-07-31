package com.ones.admin.auth.oauth;

import com.ones.admin.auth.AuthService;
import com.ones.admin.auth.dto.LoginResponse;
import com.ones.admin.auth.dto.UserProfile;
import com.ones.admin.auth.oauth.model.ExternalIdentity;
import com.ones.admin.auth.oauth.model.ExternalIdentityProfile;
import com.ones.admin.auth.oauth.repository.ExternalIdentityRepository;
import com.ones.admin.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthFlowServiceTest {

    private final MutableClock clock = new MutableClock();
    private final OAuthFlowProperties properties = new OAuthFlowProperties();
    private final MemoryOAuthFlowStore flowStore = new MemoryOAuthFlowStore(clock);
    private final StubExternalIdentityRepository identityRepository = new StubExternalIdentityRepository();
    private final AuthService authService = mock(AuthService.class);
    private final Queue<String> tokens = new ArrayDeque<>();
    private OAuthFlowService flowService;

    @BeforeEach
    void setUp() {
        properties.setStateTtl(Duration.ofMinutes(5));
        properties.setTicketTtl(Duration.ofSeconds(60));
        ThirdPartyAuthProvider provider = new StubFeishuProvider();
        flowService = new OAuthFlowService(
                new ThirdPartyAuthProviderRegistry(List.of(provider)),
                flowStore,
                identityRepository,
                authService,
                tokens::remove,
                properties
        );
    }

    @Test
    void callbackConsumesStateAndRejectsReplay() {
        identityRepository.identity = boundIdentity();
        tokens.add("state-token");
        tokens.add("login-ticket");

        OAuthAuthorizeResult authorizeResult = flowService.authorize("feishu");
        OAuthCallbackResult callbackResult = flowService.callback("feishu", "valid-code", "state-token");

        assertThat(authorizeResult.authorizationUrl()).contains("state=state-token");
        assertThat(callbackResult.ticket()).isEqualTo("login-ticket");
        assertThatThrownBy(() -> flowService.callback("feishu", "valid-code", "state-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("OAuth state 无效或已过期");
    }

    @Test
    void callbackRejectsExpiredState() {
        tokens.add("expiring-state");
        flowService.authorize("feishu");
        clock.advance(Duration.ofMinutes(6));

        assertThatThrownBy(() -> flowService.callback("feishu", "valid-code", "expiring-state"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("OAuth state 无效或已过期");
    }

    @Test
    void callbackRejectsIdentityThatHasNotBeenPreBound() {
        tokens.add("state-token");
        flowService.authorize("feishu");

        assertThatThrownBy(() -> flowService.callback("feishu", "valid-code", "state-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("飞书身份尚未绑定 ONES-ADMIN 账号");
    }

    @Test
    void exchangeConsumesTicketOnlyOnce() {
        identityRepository.identity = boundIdentity();
        tokens.add("state-token");
        tokens.add("login-ticket");
        when(authService.loginByUserId(1L)).thenReturn(loginResponse());

        flowService.authorize("feishu");
        flowService.callback("feishu", "valid-code", "state-token");
        OAuthExchangeResult exchangeResult = flowService.exchange("login-ticket");

        assertThat(exchangeResult.loginResponse().user().username()).isEqualTo("admin");
        assertThat(exchangeResult.provider()).isEqualTo("feishu");
        assertThat(identityRepository.lastLoginIdentityId).isEqualTo(10L);
        assertThatThrownBy(() -> flowService.exchange("login-ticket"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("OAuth 登录票据无效或已过期");
    }

    @Test
    void providerCapabilityDoesNotExposeSecrets() {
        assertThat(flowService.providers())
                .containsExactly(new AuthProviderCapability("feishu", "飞书", true));
    }

    private ExternalIdentity boundIdentity() {
        return new ExternalIdentity(10L, "feishu", "tenant-1", "union-1", 1L,
                ExternalIdentityStatus.ACTIVE, null);
    }

    private LoginResponse loginResponse() {
        return new LoginResponse(null, new UserProfile(1L, "admin", "系统管理员", ""),
                List.of("SUPER_ADMIN"), List.of("dashboard:view"));
    }

    private static final class StubFeishuProvider implements ThirdPartyAuthProvider {

        @Override
        public String id() {
            return "feishu";
        }

        @Override
        public String displayName() {
            return "飞书";
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public String authorizationUrl(String state) {
            return "https://open.feishu.cn/open-apis/authen/v1/index?state=" + state;
        }

        @Override
        public ExternalIdentityProfile authenticate(String authorizationCode) {
            return new ExternalIdentityProfile("feishu", "tenant-1", "union-1", "测试用户");
        }
    }

    private static final class StubExternalIdentityRepository implements ExternalIdentityRepository {

        private ExternalIdentity identity;
        private Long lastLoginIdentityId;

        @Override
        public Optional<ExternalIdentity> findActive(String provider, String tenantKey, String externalSubject) {
            return Optional.ofNullable(identity);
        }

        @Override
        public Optional<ExternalIdentity> findAny(String provider, String tenantKey, String externalSubject) {
            return Optional.ofNullable(identity);
        }

        @Override
        public List<ExternalIdentity> findByUserId(Long userId) {
            return identity == null ? List.of() : List.of(identity);
        }

        @Override
        public ExternalIdentity save(ExternalIdentity identity) {
            this.identity = identity;
            return identity;
        }

        @Override
        public void disable(Long identityId, Long userId, LocalDateTime updatedAt) {
        }

        @Override
        public void recordLogin(Long identityId, LocalDateTime loginAt) {
            lastLoginIdentityId = identityId;
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant = Instant.parse("2026-07-11T00:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
