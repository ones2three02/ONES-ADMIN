package com.ones.admin.system;

import java.util.List;
import java.util.Objects;

public record DataScopeContext(
        DataScope dataScope,
        Long userId,
        Long deptId,
        List<Long> deptIds
) {

    public boolean isAll() {
        return dataScope == DataScope.ALL;
    }

    public boolean canAccess(Long resourceDeptId, Long resourceUserId) {
        if (isAll()) {
            return true;
        }
        if (dataScope == DataScope.SELF) {
            return userId != null && Objects.equals(userId, resourceUserId);
        }
        return resourceDeptId != null && deptIds.contains(resourceDeptId);
    }
}
