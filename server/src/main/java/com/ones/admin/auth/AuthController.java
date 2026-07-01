package com.ones.admin.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.auth.dto.LoginResponse;
import com.ones.admin.auth.dto.TokenInfo;
import com.ones.admin.auth.dto.UserProfile;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.system.audit.AuditRequestContext;
import com.ones.admin.system.audit.LoginAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证中心")
@ApiResourceMetadata(
        owner = "认证与安全组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.1",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.HIGH
)
public class AuthController {

    private static final long TOKEN_TIMEOUT_SECONDS = 86_400L;

    private final AuthService authService;
    private final LoginAuditService loginAuditService;

    public AuthController(AuthService authService, LoginAuditService loginAuditService) {
        this.authService = authService;
        this.loginAuditService = loginAuditService;
    }

    @PostMapping("/login")
    @ApiAccessPolicy(value = ApiAuthType.PUBLIC, reason = "登录入口必须对未登录用户开放，并由登录失败次数和临时锁定策略降低暴力破解风险")
    @Operation(summary = "账号密码登录")
    public ApiResult<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        AuditRequestContext auditContext = AuditRequestContext.from(servletRequest);
        try {
            LoginResponse loginResponse = authService.login(request);
            StpUtil.login(loginResponse.user().id());
            loginAuditService.recordSuccess(request.username(), loginResponse.user().id(), auditContext);
            TokenInfo tokenInfo = new TokenInfo(StpUtil.getTokenName(), StpUtil.getTokenValue(), "Bearer");
            return ApiResult.ok(loginResponse.withToken(tokenInfo));
        } catch (BusinessException exception) {
            loginAuditService.recordFailure(request.username(), exception.getMessage(), auditContext);
            throw exception;
        }
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ApiResult<UserProfile> me() {
        Long userId = currentUserId();
        return ApiResult.ok(authService.getRequiredProfile(userId));
    }

    @GetMapping("/roles")
    @Operation(summary = "获取当前用户角色")
    public ApiResult<List<String>> roles() {
        return ApiResult.ok(authService.getRoleList(currentUserId()));
    }

    @GetMapping("/codes")
    @Operation(summary = "获取当前用户权限码")
    public ApiResult<List<String>> codes() {
        return ApiResult.ok(authService.getPermissionList(currentUserId()));
    }

    @PostMapping("/refresh")
    @RepeatSubmit
    @Operation(summary = "刷新当前登录令牌")
    public ApiResult<String> refresh() {
        StpUtil.checkLogin();
        StpUtil.renewTimeout(TOKEN_TIMEOUT_SECONDS);
        return ApiResult.ok(StpUtil.getTokenValue());
    }

    @PostMapping("/logout")
    @RepeatSubmit
    @Operation(summary = "退出登录")
    public ApiResult<Void> logout() {
        StpUtil.logout();
        return ApiResult.ok(null);
    }

    private Long currentUserId() {
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }
}
