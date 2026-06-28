package com.ones.admin.menu;

import com.ones.admin.common.web.ApiResult;
import com.ones.admin.menu.dto.MenuItem;
import com.ones.admin.system.MenuManagementService;
import com.ones.admin.system.dto.MenuRouteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
@Tag(name = "系统管理-当前菜单")
public class MenuController {

    private final MenuManagementService menuManagementService;

    public MenuController(MenuManagementService menuManagementService) {
        this.menuManagementService = menuManagementService;
    }

    @GetMapping
    @Operation(summary = "获取当前用户菜单树")
    public ApiResult<List<MenuItem>> listMenus() {
        return ApiResult.ok(menuManagementService.listCurrentMenuItems());
    }

    @GetMapping("/routes")
    @Operation(summary = "获取当前用户动态路由")
    public ApiResult<List<MenuRouteResponse>> listRoutes() {
        return ApiResult.ok(menuManagementService.listCurrentRoutes());
    }
}
