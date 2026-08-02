package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record DictItemResponse(
        Long id,
        Long typeId,
        String dictCode,
        String itemLabel,
        String itemValue,
        String color,
        String remark,
        boolean enabled,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
