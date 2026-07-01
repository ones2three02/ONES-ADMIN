package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.system.dto.RoleManagementResponse;
import com.ones.admin.system.dto.RoleResponse;
import com.ones.admin.system.dto.RoleSaveRequest;
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
@RequestMapping("/api/system")
@Tag(name = "系统管理-角色")
@ApiResourceMetadata(
        owner = "系统平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.1",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class RoleController {

    private final UserManagementService userManagementService;
    private final RoleManagementService roleManagementService;

    public RoleController(
            UserManagementService userManagementService,
            RoleManagementService roleManagementService
    ) {
        this.userManagementService = userManagementService;
        this.roleManagementService = roleManagementService;
    }

    @GetMapping("/roles")
    @SaCheckPermission("system:role:list")
    @Operation(summary = "查询角色选项")
    public ApiResult<List<RoleResponse>> listRoles() {
        return ApiResult.ok(userManagementService.listRoles());
    }

    @GetMapping("/role/list")
    @SaCheckPermission("system:role:list")
    @Operation(summary = "查询角色列表")
    public ApiResult<List<RoleManagementResponse>> listRoleManagementRows() {
        return ApiResult.ok(roleManagementService.listRoles());
    }

    @PostMapping("/role")
    @SaCheckPermission("system:role:create")
    @Operation(summary = "新增角色")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<RoleManagementResponse> createRole(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResult.ok(roleManagementService.create(request));
    }

    @PutMapping("/role/{id}")
    @SaCheckPermission("system:role:update")
    @Operation(summary = "编辑角色")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<RoleManagementResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleSaveRequest request
    ) {
        return ApiResult.ok(roleManagementService.update(id, request));
    }

    @DeleteMapping("/role/{id}")
    @SaCheckPermission("system:role:delete")
    @Operation(summary = "删除角色")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<Void> deleteRole(@PathVariable Long id) {
        roleManagementService.delete(id);
        return ApiResult.ok(null);
    }
}
