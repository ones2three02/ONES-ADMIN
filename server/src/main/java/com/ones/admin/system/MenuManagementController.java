package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.system.dto.MenuResponse;
import com.ones.admin.system.dto.MenuSaveRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menu")
@Tag(name = "系统管理-菜单")
public class MenuManagementController {

    private final MenuManagementService menuManagementService;

    public MenuManagementController(MenuManagementService menuManagementService) {
        this.menuManagementService = menuManagementService;
    }

    @GetMapping("/list")
    @SaCheckPermission("system:menu:list")
    @Operation(summary = "查询菜单树")
    public ApiResult<List<MenuResponse>> listMenus() {
        return ApiResult.ok(menuManagementService.listManagementTree());
    }

    @GetMapping("/name-exists")
    @SaCheckPermission("system:menu:list")
    @Operation(summary = "校验菜单名称是否存在")
    public ApiResult<Boolean> isMenuNameExists(
            @RequestParam String name,
            @RequestParam(required = false) String id
    ) {
        return ApiResult.ok(menuManagementService.isNameExists(name, parseId(id)));
    }

    @GetMapping("/path-exists")
    @SaCheckPermission("system:menu:list")
    @Operation(summary = "校验菜单路径是否存在")
    public ApiResult<Boolean> isMenuPathExists(
            @RequestParam String path,
            @RequestParam(required = false) String id
    ) {
        return ApiResult.ok(menuManagementService.isPathExists(path, parseId(id)));
    }

    @PostMapping
    @SaCheckPermission("system:menu:create")
    @Operation(summary = "新增菜单")
    @RepeatSubmit
    public ApiResult<MenuResponse> createMenu(@Valid @RequestBody MenuSaveRequest request) {
        return ApiResult.ok(menuManagementService.create(request));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:menu:update")
    @Operation(summary = "编辑菜单")
    @RepeatSubmit
    public ApiResult<MenuResponse> updateMenu(
            @PathVariable Long id,
            @Valid @RequestBody MenuSaveRequest request
    ) {
        return ApiResult.ok(menuManagementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:menu:delete")
    @Operation(summary = "删除菜单")
    @RepeatSubmit
    public ApiResult<Void> deleteMenu(@PathVariable Long id) {
        menuManagementService.delete(id);
        return ApiResult.ok(null);
    }

    private Long parseId(String id) {
        if (id == null || id.isBlank() || "0".equals(id)) {
            return null;
        }
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException exception) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "id 参数不正确");
        }
    }
}
