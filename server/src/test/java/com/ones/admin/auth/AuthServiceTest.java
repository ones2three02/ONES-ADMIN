package com.ones.admin.auth;

import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.auth.dto.LoginResponse;
import com.ones.admin.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private final AuthService authService = AuthService.createForTest();

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
}
