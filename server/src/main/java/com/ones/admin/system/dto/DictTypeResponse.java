package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record DictTypeResponse(
        Long id,
        String dictCode,
        String dictName,
        String remark,
        boolean enabled,
        Integer sortOrder,
        Long itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
