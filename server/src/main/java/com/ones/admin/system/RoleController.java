package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.system.dto.RoleManagementResponse;
import com.ones.admin.system.dto.RoleResponse;
import com.ones.admin.system.dto.RoleSaveRequest;
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
    public ApiResult<List<RoleResponse>> listRoles() {
        return ApiResult.ok(userManagementService.listRoles());
    }

    @GetMapping("/role/list")
    @SaCheckPermission("system:role:list")
    public ApiResult<List<RoleManagementResponse>> listRoleManagementRows() {
        return ApiResult.ok(roleManagementService.listRoles());
    }

    @PostMapping("/role")
    @SaCheckPermission("system:role:create")
    public ApiResult<RoleManagementResponse> createRole(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResult.ok(roleManagementService.create(request));
    }

    @PutMapping("/role/{id}")
    @SaCheckPermission("system:role:update")
    public ApiResult<RoleManagementResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleSaveRequest request
    ) {
        return ApiResult.ok(roleManagementService.update(id, request));
    }

    @DeleteMapping("/role/{id}")
    @SaCheckPermission("system:role:delete")
    public ApiResult<Void> deleteRole(@PathVariable Long id) {
        roleManagementService.delete(id);
        return ApiResult.ok(null);
    }
}
