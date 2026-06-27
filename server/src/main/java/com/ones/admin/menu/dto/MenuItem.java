package com.ones.admin.menu.dto;

import java.util.List;

public record MenuItem(
        String title,
        String path,
        String icon,
        String permission,
        List<MenuItem> children
) {
}
