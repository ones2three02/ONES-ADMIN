package com.ones.admin.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.hr.dto.HrJobGradeResponse;
import com.ones.admin.hr.dto.HrJobGradeSaveRequest;
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
@RequestMapping("/api/hr/job-grades")
@Tag(name = "HRMS-职级")
@ApiResourceMetadata(
        owner = "人力平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.51",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class HrJobGradeController {

    private final HrJobGradeService jobGradeService;

    public HrJobGradeController(HrJobGradeService jobGradeService) {
        this.jobGradeService = jobGradeService;
    }

    @GetMapping
    @SaCheckPermission("hr:job-grade:list")
    @Operation(operationId = "HrJobGradeController_listJobGrades", summary = "查询职级列表")
    public ApiResult<List<HrJobGradeResponse>> listJobGrades() {
        return ApiResult.ok(jobGradeService.listJobGrades());
    }

    @PostMapping
    @SaCheckPermission("hr:job-grade:create")
    @Operation(operationId = "HrJobGradeController_createJobGrade", summary = "新增职级")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrJobGradeResponse> createJobGrade(@Valid @RequestBody HrJobGradeSaveRequest request) {
        return ApiResult.ok(jobGradeService.createJobGrade(request));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("hr:job-grade:update")
    @Operation(operationId = "HrJobGradeController_updateJobGrade", summary = "编辑职级")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrJobGradeResponse> updateJobGrade(
            @PathVariable Long id,
            @Valid @RequestBody HrJobGradeSaveRequest request
    ) {
        return ApiResult.ok(jobGradeService.updateJobGrade(id, request));
    }
}
