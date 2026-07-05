package com.ones.admin.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.hr.dto.HrEmployeeCreateRequest;
import com.ones.admin.hr.dto.HrEmployeeLifecycleEventResponse;
import com.ones.admin.hr.dto.HrEmployeeQuery;
import com.ones.admin.hr.dto.HrEmployeeRegularizeRequest;
import com.ones.admin.hr.dto.HrEmployeeResignRequest;
import com.ones.admin.hr.dto.HrEmployeeResponse;
import com.ones.admin.hr.dto.HrEmployeeTransferRequest;
import com.ones.admin.hr.dto.HrEmployeeUpdateRequest;
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
@RequestMapping("/api/hr/employees")
@Tag(name = "HRMS-员工档案")
@ApiResourceMetadata(
        owner = "人力平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.51",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class HrEmployeeController {

    private final HrEmployeeService employeeService;

    public HrEmployeeController(HrEmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @SaCheckPermission("hr:employee:list")
    @Operation(operationId = "HrEmployeeController_listEmployees", summary = "查询员工列表")
    public ApiResult<PageResult<HrEmployeeResponse>> listEmployees(@Valid HrEmployeeQuery query) {
        return ApiResult.ok(employeeService.queryEmployees(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("hr:employee:detail")
    @Operation(operationId = "HrEmployeeController_getEmployee", summary = "查询员工详情")
    public ApiResult<HrEmployeeResponse> getEmployee(@PathVariable Long id) {
        return ApiResult.ok(employeeService.getEmployee(id));
    }

    @GetMapping("/{id}/lifecycle-events")
    @SaCheckPermission("hr:employee:lifecycle")
    @Operation(operationId = "HrEmployeeController_listEmployeeLifecycleEvents", summary = "查询员工生命周期事件")
    @ApiResourceMetadata(sinceVersion = "v0.0.54", riskLevel = ApiRiskLevel.MEDIUM)
    public ApiResult<List<HrEmployeeLifecycleEventResponse>> listLifecycleEvents(@PathVariable Long id) {
        return ApiResult.ok(employeeService.listLifecycleEvents(id));
    }

    @PostMapping
    @SaCheckPermission("hr:employee:create")
    @Operation(operationId = "HrEmployeeController_createEmployee", summary = "新增员工")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrEmployeeResponse> createEmployee(@Valid @RequestBody HrEmployeeCreateRequest request) {
        return ApiResult.ok(employeeService.createEmployee(request));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("hr:employee:update")
    @Operation(operationId = "HrEmployeeController_updateEmployee", summary = "编辑员工基础信息")
    @RepeatSubmit
    @ApiResourceMetadata(sinceVersion = "v0.0.65", riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrEmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody HrEmployeeUpdateRequest request
    ) {
        return ApiResult.ok(employeeService.updateEmployee(id, request));
    }

    @PostMapping("/{id}/transfer")
    @SaCheckPermission("hr:employee:transfer")
    @Operation(operationId = "HrEmployeeController_transferEmployee", summary = "员工调岗")
    @RepeatSubmit
    @ApiResourceMetadata(sinceVersion = "v0.0.52", riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrEmployeeResponse> transferEmployee(
            @PathVariable Long id,
            @Valid @RequestBody HrEmployeeTransferRequest request
    ) {
        return ApiResult.ok(employeeService.transferEmployee(id, request));
    }

    @PostMapping("/{id}/regularize")
    @SaCheckPermission("hr:employee:regularize")
    @Operation(operationId = "HrEmployeeController_regularizeEmployee", summary = "员工转正")
    @RepeatSubmit
    @ApiResourceMetadata(sinceVersion = "v0.0.52", riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrEmployeeResponse> regularizeEmployee(
            @PathVariable Long id,
            @Valid @RequestBody HrEmployeeRegularizeRequest request
    ) {
        return ApiResult.ok(employeeService.regularizeEmployee(id, request));
    }

    @PostMapping("/{id}/resign")
    @SaCheckPermission("hr:employee:resign")
    @Operation(operationId = "HrEmployeeController_resignEmployee", summary = "员工离职")
    @RepeatSubmit
    @ApiResourceMetadata(sinceVersion = "v0.0.52", riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrEmployeeResponse> resignEmployee(
            @PathVariable Long id,
            @Valid @RequestBody HrEmployeeResignRequest request
    ) {
        return ApiResult.ok(employeeService.resignEmployee(id, request));
    }
}
