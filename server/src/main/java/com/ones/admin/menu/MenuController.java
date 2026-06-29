package com.ones.admin.menu;

import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
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
@ApiAccessPolicy(value = ApiAuthType.LOGIN, reason = "当前用户菜单和动态路由由登录态上下文生成，不需要额外按钮权限")
@ApiResourceMetadata(
        owner = "前端平台组",
        sinceVersion = "v0.0.1",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
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
