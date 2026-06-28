package com.ones.admin.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuSaveRequest(
        String pid,
        @NotBlank @Size(max = 64) String name,
        @Size(max = 255) String path,
        @Size(max = 255) String component,
        @Size(max = 255) String redirect,
        @Size(max = 128) String authCode,
        @NotBlank String type,
        Integer status,
        MenuMetaRequest meta
) {

    public record MenuMetaRequest(
            @Size(max = 128) String title,
            @Size(max = 128) String icon,
            Integer order,
            Boolean hideInMenu,
            Boolean hideInBreadcrumb,
            Boolean hideInTab,
            Boolean hideChildrenInMenu,
            Boolean keepAlive,
            Boolean affixTab
    ) {
    }
}
