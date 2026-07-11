package com.ones.admin.auth;

import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.auth.dto.LoginResponse;
import com.ones.admin.auth.model.AdminUser;
import com.ones.admin.auth.repository.UserRepository;
import com.ones.admin.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AuthService authService = createAuthService();

    @Test
    void loginReturnsUserRolesAndPermissionsWhenPasswordIsValid() {
        LoginResponse response = authService.login(new LoginRequest("admin", "admin123"));

        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().displayName()).isEqualTo("系统管理员");
        assertThat(response.roles()).containsExactly("SUPER_ADMIN");
        assertThat(response.permissions()).contains("system:user:list", "system:menu:list");
    }

    @Test
    void loginRejectsInvalidPassword() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void loginLocksAccountAfterRepeatedFailures() {
        MutableUserRepository repository = new MutableUserRepository(new AdminUser(
                2L,
                "locked-user",
                "锁定测试用户",
                "",
                passwordEncoder.encode("right-password"),
                List.of("OPERATOR"),
                List.of("dashboard:view"),
                true,
                0,
                null
        ));
        AuthService service = new AuthService(repository, passwordEncoder);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login(new LoginRequest("locked-user", "wrong-password")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("用户名或密码错误");
        }

        assertThat(repository.user.failedLoginCount()).isEqualTo(5);
        assertThat(repository.user.lockedUntil()).isAfter(LocalDateTime.now());
        assertThatThrownBy(() -> service.login(new LoginRequest("locked-user", "right-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已被临时锁定，请稍后再试");
    }

    private AuthService createAuthService() {
        return new AuthService(new MutableUserRepository(new AdminUser(
                1L,
                "admin",
                "系统管理员",
                "",
                passwordEncoder.encode("admin123"),
                List.of("SUPER_ADMIN"),
                List.of("dashboard:view", "system:user:list", "system:menu:list"),
                true,
                0,
                null
        )), passwordEncoder);
    }

    private static final class MutableUserRepository implements UserRepository {

        private AdminUser user;

        private MutableUserRepository(AdminUser user) {
            this.user = user;
        }

        @Override
        public Optional<AdminUser> findByUsername(String username) {
            return user.username().equals(username) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public Optional<AdminUser> findById(Long id) {
            return user.id().equals(id) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public void recordLoginSuccess(Long userId) {
            user = copyWithLoginState(0, null);
        }

        @Override
        public void recordLoginFailure(Long userId, int maxFailedLoginCount, LocalDateTime lockedUntil) {
            int failedLoginCount = user.failedLoginCount() + 1;
            user = copyWithLoginState(
                    failedLoginCount,
                    failedLoginCount >= maxFailedLoginCount ? lockedUntil : null
            );
        }

        private AdminUser copyWithLoginState(int failedLoginCount, LocalDateTime lockedUntil) {
            return new AdminUser(
                    user.id(),
                    user.username(),
                    user.displayName(),
                    user.avatar(),
                    user.passwordHash(),
                    user.roles(),
                    user.permissions(),
                    user.enabled(),
                    failedLoginCount,
                    lockedUntil
            );
        }
    }
}
