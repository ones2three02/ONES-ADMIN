package com.ones.admin.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.auth.dto.LoginResponse;
import com.ones.admin.auth.dto.TokenInfo;
import com.ones.admin.auth.dto.UserProfile;
import com.ones.admin.common.web.ApiResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        StpUtil.login(loginResponse.user().id());
        TokenInfo tokenInfo = new TokenInfo(StpUtil.getTokenName(), StpUtil.getTokenValue(), "Bearer");
        return ApiResult.ok(loginResponse.withToken(tokenInfo));
    }

    @GetMapping("/me")
    public ApiResult<UserProfile> me() {
        Long userId = Long.valueOf(String.valueOf(StpUtil.getLoginId()));
        return ApiResult.ok(authService.getRequiredProfile(userId));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        StpUtil.logout();
        return ApiResult.ok(null);
    }
}
