package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.system.dto.UserCreateRequest;
import com.ones.admin.system.dto.UserResponse;
import com.ones.admin.system.dto.UserUpdateRequest;
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
@RequestMapping("/api/system/users")
@Tag(name = "系统管理-用户")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @SaCheckPermission("system:user:list")
    @Operation(summary = "查询用户列表")
    public ApiResult<List<UserResponse>> listUsers() {
        return ApiResult.ok(userManagementService.listUsers());
    }

    @PostMapping
    @SaCheckPermission("system:user:create")
    @Operation(summary = "新增用户")
    public ApiResult<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResult.ok(userManagementService.createUser(request));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:user:update")
    @Operation(summary = "编辑用户")
    public ApiResult<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResult.ok(userManagementService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:user:delete")
    @Operation(summary = "删除用户")
    public ApiResult<Void> deleteUser(@PathVariable Long id) {
        userManagementService.deleteUser(id);
        return ApiResult.ok(null);
    }
}
