package com.ones.admin.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.hr.dto.HrOverviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/overview")
@Tag(name = "HRMS-人力概览")
@ApiResourceMetadata(
        owner = "人力平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.70",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class HrOverviewController {

    private final HrOverviewService overviewService;

    public HrOverviewController(HrOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping
    @SaCheckPermission("hr:overview:view")
    @Operation(operationId = "HrOverviewController_getOverview", summary = "查询人力概览")
    public ApiResult<HrOverviewResponse> getOverview() {
        return ApiResult.ok(overviewService.getOverview());
    }
}
