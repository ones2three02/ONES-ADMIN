package com.ones.admin.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.hr.dto.HrPositionResponse;
import com.ones.admin.hr.dto.HrPositionSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr/positions")
@Tag(name = "HRMS-岗位")
@ApiResourceMetadata(
        owner = "人力平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.51",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class HrPositionController {

    private final HrPositionService positionService;

    public HrPositionController(HrPositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    @SaCheckPermission("hr:position:list")
    @Operation(operationId = "HrPositionController_listPositions", summary = "查询岗位列表")
    public ApiResult<List<HrPositionResponse>> listPositions() {
        return ApiResult.ok(positionService.listPositions());
    }

    @PostMapping
    @SaCheckPermission("hr:position:create")
    @Operation(operationId = "HrPositionController_createPosition", summary = "新增岗位")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrPositionResponse> createPosition(@Valid @RequestBody HrPositionSaveRequest request) {
        return ApiResult.ok(positionService.createPosition(request));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("hr:position:update")
    @Operation(operationId = "HrPositionController_updatePosition", summary = "编辑岗位")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrPositionResponse> updatePosition(
            @PathVariable Long id,
            @Valid @RequestBody HrPositionSaveRequest request
    ) {
        return ApiResult.ok(positionService.updatePosition(id, request));
    }
}
