package com.ones.admin.system;

import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "系统健康")
@ApiResourceMetadata(
        owner = "运维平台组",
        sinceVersion = "v0.0.1",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.LOW
)
public class HealthController {

    @GetMapping
    @ApiAccessPolicy(value = ApiAuthType.PUBLIC, reason = "健康检查用于负载均衡、发布探针和基础可用性巡检，只返回非敏感状态")
    @Operation(summary = "健康检查")
    public ApiResult<Map<String, String>> health() {
        return ApiResult.ok(Map.of("status", "UP", "service", "ones-admin-server"));
    }
}
