package com.ones.admin.system.dto;

public record DictOptionResponse(
        String label,
        String value,
        String color,
        Integer sortOrder
) {
}
