package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.system.dto.MenuResponse;
import com.ones.admin.system.dto.MenuSaveRequest;
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
public class MenuManagementController {

    private final MenuManagementService menuManagementService;

    public MenuManagementController(MenuManagementService menuManagementService) {
        this.menuManagementService = menuManagementService;
    }

    @GetMapping("/list")
    @SaCheckPermission("system:menu:list")
    public ApiResult<List<MenuResponse>> listMenus() {
        return ApiResult.ok(menuManagementService.listManagementTree());
    }

    @GetMapping("/name-exists")
    @SaCheckPermission("system:menu:list")
    public ApiResult<Boolean> isMenuNameExists(
            @RequestParam String name,
            @RequestParam(required = false) String id
    ) {
        return ApiResult.ok(menuManagementService.isNameExists(name, parseId(id)));
    }

    @GetMapping("/path-exists")
    @SaCheckPermission("system:menu:list")
    public ApiResult<Boolean> isMenuPathExists(
            @RequestParam String path,
            @RequestParam(required = false) String id
    ) {
        return ApiResult.ok(menuManagementService.isPathExists(path, parseId(id)));
    }

    @PostMapping
    @SaCheckPermission("system:menu:create")
    public ApiResult<MenuResponse> createMenu(@Valid @RequestBody MenuSaveRequest request) {
        return ApiResult.ok(menuManagementService.create(request));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:menu:update")
    public ApiResult<MenuResponse> updateMenu(
            @PathVariable Long id,
            @Valid @RequestBody MenuSaveRequest request
    ) {
        return ApiResult.ok(menuManagementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:menu:delete")
    public ApiResult<Void> deleteMenu(@PathVariable Long id) {
        menuManagementService.delete(id);
        return ApiResult.ok(null);
    }

    private Long parseId(String id) {
        if (id == null || id.isBlank() || "0".equals(id)) {
            return null;
        }
        return Long.valueOf(id);
    }
}
