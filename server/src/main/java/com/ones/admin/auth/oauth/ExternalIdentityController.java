package com.ones.admin.auth.oauth;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/users/{userId}/external-identities")
@Tag(name = "系统管理-外部身份")
@ApiResourceMetadata(
        owner = "认证与安全组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.149",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.HIGH
)
public class ExternalIdentityController {

    private final ExternalIdentityService identityService;

    public ExternalIdentityController(ExternalIdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping
    @SaCheckPermission("system:user:update")
    @Operation(summary = "查询用户外部身份")
    public ApiResult<List<ExternalIdentityResponse>> list(@PathVariable Long userId) {
        return ApiResult.ok(identityService.list(userId));
    }

    @PostMapping
    @SaCheckPermission("system:user:update")
    @RepeatSubmit
    @Operation(summary = "预绑定用户外部身份")
    public ApiResult<ExternalIdentityResponse> bind(
            @PathVariable Long userId,
            @Valid @RequestBody ExternalIdentityBindRequest request
    ) {
        return ApiResult.ok(identityService.bind(userId, request));
    }

    @DeleteMapping("/{identityId}")
    @SaCheckPermission("system:user:update")
    @RepeatSubmit
    @Operation(summary = "停用用户外部身份")
    public ApiResult<Void> unbind(@PathVariable Long userId, @PathVariable Long identityId) {
        identityService.unbind(userId, identityId);
        return ApiResult.ok(null);
    }
}
