package com.ones.admin.system.dto;

public record MenuMetaResponse(
        String title,
        String icon,
        Integer order,
        Boolean hideInMenu,
        Boolean hideInBreadcrumb,
        Boolean hideInTab,
        Boolean hideChildrenInMenu,
        Boolean keepAlive,
        Boolean affixTab
) {
}
