package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.ApiResourceGovernanceResponse;
import com.ones.admin.system.dto.ApiResourceManifestDiffResponse;
import com.ones.admin.system.dto.ApiResourceManifestGateRequest;
import com.ones.admin.system.dto.ApiResourceManifestGateResponse;
import com.ones.admin.system.dto.ApiResourceManifestResponse;
import com.ones.admin.system.dto.ApiResourceQuery;
import com.ones.admin.system.dto.ApiResourceResponse;
import com.ones.admin.system.dto.ApiResourceSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/system/api-resources")
@Tag(name = "系统管理-接口资源")
@ApiResourceMetadata(
        owner = "架构治理组",
        sinceVersion = "v0.0.7",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class ApiResourceController {

    private final ApiResourceService apiResourceService;

    public ApiResourceController(ApiResourceService apiResourceService) {
        this.apiResourceService = apiResourceService;
    }

    @GetMapping
    @SaCheckPermission("system:api:list")
    @Operation(summary = "查询接口资源")
    public ApiResult<PageResult<ApiResourceResponse>> listApiResources(@Valid ApiResourceQuery query) {
        return ApiResult.ok(apiResourceService.queryPage(query));
    }

    @GetMapping("/summary")
    @SaCheckPermission("system:api:list")
    @Operation(summary = "查询接口资源汇总")
    public ApiResult<ApiResourceSummaryResponse> summarizeApiResources() {
        return ApiResult.ok(apiResourceService.summarize());
    }

    @GetMapping("/governance")
    @SaCheckPermission("system:api:list")
    @Operation(summary = "查询接口治理质量门禁")
    public ApiResult<ApiResourceGovernanceResponse> checkApiResourceGovernance() {
        return ApiResult.ok(apiResourceService.checkGovernance());
    }

    @GetMapping("/manifest")
    @SaCheckPermission("system:api:list")
    @Operation(summary = "生成接口资源 Manifest")
    public ApiResult<ApiResourceManifestResponse> generateApiResourceManifest() {
        return ApiResult.ok(apiResourceService.generateManifest());
    }

    @PostMapping("/manifest/diff")
    @SaCheckPermission("system:api:list")
    @Operation(summary = "对比接口资源 Manifest")
    public ApiResult<ApiResourceManifestDiffResponse> diffApiResourceManifest(
            @RequestBody ApiResourceManifestResponse previousManifest
    ) {
        return ApiResult.ok(apiResourceService.diffManifest(previousManifest));
    }

    @PostMapping("/manifest/gate")
    @SaCheckPermission("system:api:list")
    @Operation(summary = "校验接口资源 Manifest 发布门禁")
    public ApiResult<ApiResourceManifestGateResponse> gateApiResourceManifest(
            @RequestBody ApiResourceManifestGateRequest request
    ) {
        return ApiResult.ok(apiResourceService.gateManifest(request));
    }

    @GetMapping("/export")
    @SaCheckPermission("system:api:list")
    @Operation(summary = "导出接口资源清单")
    public ResponseEntity<String> exportApiResources() {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ones-api-resources.csv\"")
                .body(apiResourceService.exportCsv());
    }
}
