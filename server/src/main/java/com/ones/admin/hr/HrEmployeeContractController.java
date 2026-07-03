package com.ones.admin.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.hr.dto.HrEmployeeContractResponse;
import com.ones.admin.hr.dto.HrEmployeeContractSaveRequest;
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
@RequestMapping("/api/hr/employees/{employeeId}/contracts")
@Tag(name = "HRMS-员工合同")
@ApiResourceMetadata(
        owner = "人力平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.55",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class HrEmployeeContractController {

    private final HrEmployeeContractService contractService;

    public HrEmployeeContractController(HrEmployeeContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    @SaCheckPermission("hr:contract:list")
    @Operation(operationId = "HrEmployeeContractController_listContracts", summary = "查询员工合同列表")
    public ApiResult<List<HrEmployeeContractResponse>> listContracts(@PathVariable Long employeeId) {
        return ApiResult.ok(contractService.listContracts(employeeId));
    }

    @PostMapping
    @SaCheckPermission("hr:contract:create")
    @Operation(operationId = "HrEmployeeContractController_createContract", summary = "新增员工合同")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrEmployeeContractResponse> createContract(
            @PathVariable Long employeeId,
            @Valid @RequestBody HrEmployeeContractSaveRequest request
    ) {
        return ApiResult.ok(contractService.createContract(employeeId, request));
    }

    @PutMapping("/{contractId}")
    @SaCheckPermission("hr:contract:update")
    @Operation(operationId = "HrEmployeeContractController_updateContract", summary = "编辑员工合同")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrEmployeeContractResponse> updateContract(
            @PathVariable Long employeeId,
            @PathVariable Long contractId,
            @Valid @RequestBody HrEmployeeContractSaveRequest request
    ) {
        return ApiResult.ok(contractService.updateContract(employeeId, contractId, request));
    }
}
