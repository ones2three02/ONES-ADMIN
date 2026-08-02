package com.ones.admin.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DictItemSaveRequest(
        Long typeId,
        @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$") String dictCode,
        @NotBlank @Size(max = 128) String itemLabel,
        @NotBlank @Size(max = 128) String itemValue,
        @Size(max = 32) String color,
        @Size(max = 255) String remark,
        Boolean enabled,
        Integer sortOrder
) {
}
