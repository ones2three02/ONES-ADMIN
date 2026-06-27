package com.ones.admin.menu;

import com.ones.admin.menu.dto.MenuItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    public List<MenuItem> getCurrentMenus() {
        return List.of(
                new MenuItem("系统管理", "/system", "Setting", "system:menu:list", List.of(
                        new MenuItem("用户管理", "/system/user", "User", "system:user:list", List.of()),
                        new MenuItem("角色管理", "/system/role", "UserFilled", "system:role:list", List.of()),
                        new MenuItem("菜单管理", "/system/menu", "Menu", "system:menu:list", List.of())
                )),
                new MenuItem("工作台", "/dashboard", "Monitor", "dashboard:view", List.of())
        );
    }
}
