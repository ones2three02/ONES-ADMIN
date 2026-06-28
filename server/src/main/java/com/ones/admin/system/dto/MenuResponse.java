package com.ones.admin.system.dto;

import java.util.List;

public record MenuResponse(
        String id,
        String pid,
        String name,
        String path,
        String component,
        String redirect,
        String authCode,
        String type,
        int status,
        MenuMetaResponse meta,
        List<MenuResponse> children
) {
}
