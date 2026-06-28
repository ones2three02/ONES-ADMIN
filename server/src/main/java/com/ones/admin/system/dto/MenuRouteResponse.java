package com.ones.admin.system.dto;

import java.util.List;

public record MenuRouteResponse(
        String name,
        String path,
        String component,
        String redirect,
        MenuMetaResponse meta,
        List<MenuRouteResponse> children
) {
}
