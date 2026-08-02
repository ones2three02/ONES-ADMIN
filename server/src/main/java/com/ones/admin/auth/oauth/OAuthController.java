package com.ones.admin.auth.oauth;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.auth.dto.LoginResponse;
import com.ones.admin.auth.dto.TokenInfo;
import com.ones.admin.common.exception.BusinessException;
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
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证中心-第三方登录")
@ApiResourceMetadata(
        owner = "认证与安全组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.149",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.HIGH
)
public class OAuthController {

    private final OAuthFlowService flowService;
    private final LoginAuditService loginAuditService;

    public OAuthController(OAuthFlowService flowService, LoginAuditService loginAuditService) {
        this.flowService = flowService;
        this.loginAuditService = loginAuditService;
    }

    @GetMapping("/providers")
    @ApiAccessPolicy(value = ApiAuthType.PUBLIC, reason = "登录页需要读取第三方认证能力状态，响应不包含任何 Secret")
    @Operation(summary = "查询第三方登录能力")
    public ApiResult<List<AuthProviderCapability>> providers() {
        return ApiResult.ok(flowService.providers());
    }

    @GetMapping("/oauth/{provider}/authorize")
    @ApiAccessPolicy(value = ApiAuthType.PUBLIC, reason = "未登录用户需要从此入口获取服务端生成的 OAuth state 和授权地址")
    @Operation(summary = "生成第三方授权地址")
    public ApiResult<OAuthAuthorizeResult> authorize(@PathVariable String provider) {
        return ApiResult.ok(flowService.authorize(provider));
    }

    @GetMapping("/oauth/{provider}/callback")
    @ApiAccessPolicy(value = ApiAuthType.PUBLIC, reason = "第三方平台回调入口，通过一次性 state 防止伪造和重放")
    @Operation(summary = "处理第三方授权回调")
    public ApiResult<OAuthCallbackResult> callback(
            @PathVariable String provider,
            @RequestParam @NotBlank String code,
            @RequestParam @NotBlank String state,
            HttpServletRequest request
    ) {
        AuditRequestContext context = AuditRequestContext.from(request);
        try {
            return ApiResult.ok(flowService.callback(provider, code, state));
        } catch (BusinessException exception) {
            loginAuditService.recordOAuthFailure(provider, exception.getMessage(), context);
            throw exception;
        }
    }

    @PostMapping("/oauth/exchange")
    @ApiAccessPolicy(value = ApiAuthType.PUBLIC, reason = "前端使用短期一次性票据换取 Sa-Token，票据消费后立即失效")
    @Operation(summary = "兑换第三方登录票据")
    public ApiResult<LoginResponse> exchange(
            @Valid @RequestBody OAuthExchangeRequest request,
            HttpServletRequest servletRequest
    ) {
        AuditRequestContext context = AuditRequestContext.from(servletRequest);
        try {
            OAuthExchangeResult result = flowService.exchange(request.ticket());
            LoginResponse loginResponse = result.loginResponse();
            StpUtil.login(loginResponse.user().id());
            loginAuditService.recordOAuthSuccess(
                    loginResponse.user().username(),
                    loginResponse.user().id(),
                    result.provider(),
                    result.externalIdentityId(),
                    context
            );
            TokenInfo tokenInfo = new TokenInfo(StpUtil.getTokenName(), StpUtil.getTokenValue(), "Bearer");
            return ApiResult.ok(loginResponse.withToken(tokenInfo));
        } catch (BusinessException exception) {
            loginAuditService.recordOAuthFailure("oauth", exception.getMessage(), context);
            throw exception;
        }
    }
}
