package com.ones.admin.menu;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.menu.dto.MenuItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    @SaCheckPermission("system:menu:list")
    public ApiResult<List<MenuItem>> listMenus() {
        return ApiResult.ok(menuService.getCurrentMenus());
    }
}
