package com.ones.admin.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.hr.dto.HrEmployeeCreateRequest;
import com.ones.admin.hr.dto.HrEmployeeQuery;
import com.ones.admin.hr.dto.HrEmployeeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping
    @SaCheckPermission("hr:employee:create")
    @Operation(operationId = "HrEmployeeController_createEmployee", summary = "新增员工")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrEmployeeResponse> createEmployee(@Valid @RequestBody HrEmployeeCreateRequest request) {
        return ApiResult.ok(employeeService.createEmployee(request));
    }
}
