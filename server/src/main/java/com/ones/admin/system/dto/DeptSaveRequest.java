package com.ones.admin.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeptSaveRequest(
        Long pid,
        @NotBlank @Size(max = 64) String name,
        @Size(max = 255) String remark,
        Integer status
) {
}
