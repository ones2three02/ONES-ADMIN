package com.ones.admin.system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeptResponse(
        String id,
        String pid,
        String name,
        String remark,
        int status,
        LocalDateTime createTime,
        List<DeptResponse> children
) {
}
