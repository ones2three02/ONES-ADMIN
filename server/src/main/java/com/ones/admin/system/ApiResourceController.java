package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.system.dto.ApiResourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ApiResult<List<ApiResourceResponse>> listApiResources() {
        return ApiResult.ok(apiResourceService.listApiResources());
    }
}
