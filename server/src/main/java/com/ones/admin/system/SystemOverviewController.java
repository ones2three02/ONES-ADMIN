package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.system.dto.SystemOverviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/overview")
@Tag(name = "系统管理-概览")
public class SystemOverviewController {

    private final SystemOverviewService systemOverviewService;

    public SystemOverviewController(SystemOverviewService systemOverviewService) {
        this.systemOverviewService = systemOverviewService;
    }

    @GetMapping
    @SaCheckPermission("dashboard:view")
    @Operation(summary = "查询系统概览")
    public ApiResult<SystemOverviewResponse> getOverview() {
        return ApiResult.ok(systemOverviewService.getOverview());
    }
}
