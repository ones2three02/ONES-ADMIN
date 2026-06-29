package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.ApiResourceGovernanceResponse;
import com.ones.admin.system.dto.ApiResourceQuery;
import com.ones.admin.system.dto.ApiResourceResponse;
import com.ones.admin.system.dto.ApiResourceSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/api-resources")
@Tag(name = "系统管理-接口资源")
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
}
