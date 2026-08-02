package com.ones.admin.hr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HrEmployeeUpdateRequest(
        @NotBlank @Size(max = 100) String realName,
        @Size(max = 100) String preferredName,
        @Size(max = 20) String gender,
        @Size(max = 32) String mobile,
        @Email @Size(max = 128) String email,
        @Size(max = 64) String idCardNumber,
        Long userId,
        LocalDate probationEndDate,
        @Size(max = 500) String remark
) {
}
