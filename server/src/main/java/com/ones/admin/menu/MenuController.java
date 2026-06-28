package com.ones.admin.menu;

import com.ones.admin.common.web.ApiResult;
import com.ones.admin.menu.dto.MenuItem;
import com.ones.admin.system.MenuManagementService;
import com.ones.admin.system.dto.MenuRouteResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
public class MenuController {

    private final MenuManagementService menuManagementService;

    public MenuController(MenuManagementService menuManagementService) {
        this.menuManagementService = menuManagementService;
    }

    @GetMapping
    public ApiResult<List<MenuItem>> listMenus() {
        return ApiResult.ok(menuManagementService.listCurrentMenuItems());
    }

    @GetMapping("/routes")
    public ApiResult<List<MenuRouteResponse>> listRoutes() {
        return ApiResult.ok(menuManagementService.listCurrentRoutes());
    }
}
