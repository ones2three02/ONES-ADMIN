package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.system.dto.DeptResponse;
import com.ones.admin.system.dto.DeptSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/dept")
@Tag(name = "系统管理-部门")
@ApiResourceMetadata(
        owner = "系统平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.1",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class DeptController {

    private final DeptManagementService deptManagementService;

    public DeptController(DeptManagementService deptManagementService) {
        this.deptManagementService = deptManagementService;
    }

    @GetMapping("/list")
    @SaCheckPermission("system:dept:list")
    @Operation(summary = "查询部门树")
    public ApiResult<List<DeptResponse>> listDeptTree() {
        return ApiResult.ok(deptManagementService.listTree());
    }

    @PostMapping
    @SaCheckPermission("system:dept:create")
    @Operation(summary = "新增部门")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<DeptResponse> createDept(@Valid @RequestBody DeptSaveRequest request) {
        return ApiResult.ok(deptManagementService.create(request));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:dept:update")
    @Operation(summary = "编辑部门")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<DeptResponse> updateDept(
            @PathVariable Long id,
            @Valid @RequestBody DeptSaveRequest request
    ) {
        return ApiResult.ok(deptManagementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:dept:delete")
    @Operation(summary = "删除部门")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<Void> deleteDept(@PathVariable Long id) {
        deptManagementService.delete(id);
        return ApiResult.ok(null);
    }
}
