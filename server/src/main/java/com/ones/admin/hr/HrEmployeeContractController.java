package com.ones.admin.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.hr.dto.HrEmployeeContractResponse;
import com.ones.admin.hr.dto.HrEmployeeContractSaveRequest;
import com.ones.admin.hr.dto.HrEmployeeContractTerminateRequest;
import com.ones.admin.system.dto.FileMetadataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hr")
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

    @GetMapping("/employees/{employeeId}/contracts")
    @SaCheckPermission("hr:contract:list")
    @Operation(operationId = "HrEmployeeContractController_listContracts", summary = "查询员工合同列表")
    public ApiResult<List<HrEmployeeContractResponse>> listContracts(@PathVariable Long employeeId) {
        return ApiResult.ok(contractService.listContracts(employeeId));
    }

    @GetMapping("/contracts/expiring")
    @SaCheckPermission("hr:contract:list")
    @Operation(operationId = "HrEmployeeContractController_listExpiringContracts", summary = "查询即将到期合同")
    @ApiResourceMetadata(sinceVersion = "v0.0.56", riskLevel = ApiRiskLevel.MEDIUM)
    public ApiResult<List<HrEmployeeContractResponse>> listExpiringContracts(
            @RequestParam(required = false) Integer days
    ) {
        return ApiResult.ok(contractService.listExpiringContracts(days));
    }

    @GetMapping("/contracts/{contractId}/attachment/metadata")
    @SaCheckPermission("hr:contract:list")
    @Operation(operationId = "HrEmployeeContractController_getAttachmentMetadata", summary = "查询合同附件元数据")
    @ApiResourceMetadata(sinceVersion = "v0.0.84", riskLevel = ApiRiskLevel.MEDIUM)
    public ApiResult<FileMetadataResponse> getAttachmentMetadata(@PathVariable Long contractId) {
        return ApiResult.ok(contractService.getAttachmentMetadata(contractId));
    }

    @PostMapping("/employees/{employeeId}/contracts")
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

    @PutMapping("/employees/{employeeId}/contracts/{contractId}")
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

    @PostMapping("/contracts/{contractId}/terminate")
    @SaCheckPermission("hr:contract:terminate")
    @Operation(operationId = "HrEmployeeContractController_terminateContract", summary = "终止员工合同")
    @RepeatSubmit
    @ApiResourceMetadata(sinceVersion = "v0.0.56", riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrEmployeeContractResponse> terminateContract(
            @PathVariable Long contractId,
            @Valid @RequestBody HrEmployeeContractTerminateRequest request
    ) {
        return ApiResult.ok(contractService.terminateContract(contractId, request));
    }
}
