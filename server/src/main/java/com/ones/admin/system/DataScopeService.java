package com.ones.admin.system;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
import com.ones.admin.system.mapper.SystemRoleMapper;
import com.ones.admin.system.mapper.SystemUserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataScopeService {

    private final SystemRoleMapper roleMapper;
    private final SystemUserMapper userMapper;
    private final SystemDeptMapper deptMapper;

    public DataScopeService(
            SystemRoleMapper roleMapper,
            SystemUserMapper userMapper,
            SystemDeptMapper deptMapper
    ) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
    }

    public DataScopeContext currentContext() {
        if (!StpUtil.isLogin()) {
            return new DataScopeContext(DataScope.SELF, null, null, List.of());
        }
        Long userId = Long.valueOf(String.valueOf(StpUtil.getLoginId()));
        SystemUserEntity user = userMapper.selectById(userId);
        Long deptId = user == null ? null : user.getDeptId();
        DataScope dataScope = mostPermissiveScope(roleMapper.selectDataScopesByUserId(userId));
        List<Long> deptIds = scopedDeptIds(dataScope, deptId);
        return new DataScopeContext(dataScope, userId, deptId, deptIds);
    }

    public boolean canAccess(Long resourceDeptId, Long resourceUserId) {
        return currentContext().canAccess(resourceDeptId, resourceUserId);
    }

    private DataScope mostPermissiveScope(List<String> roleScopes) {
        DataScope result = DataScope.SELF;
        for (String roleScope : roleScopes) {
            DataScope scope = safeScope(roleScope);
            if (scope.broaderThan(result)) {
                result = scope;
            }
        }
        return result;
    }

    private DataScope safeScope(String value) {
        try {
            return DataScope.from(value);
        } catch (IllegalArgumentException ex) {
            return DataScope.SELF;
        }
    }

    private List<Long> scopedDeptIds(DataScope dataScope, Long deptId) {
        if (dataScope == DataScope.ALL) {
            return List.of();
        }
        if (deptId == null) {
            return List.of();
        }
        if (dataScope == DataScope.DEPT) {
            return List.of(deptId);
        }
        if (dataScope == DataScope.SELF) {
            return List.of();
        }
        return descendantDeptIds(deptId);
    }

    private List<Long> descendantDeptIds(Long rootDeptId) {
        List<SystemDeptEntity> depts = deptMapper.selectList(null);
        Map<Long, List<SystemDeptEntity>> childrenByParentId = depts.stream()
                .filter(dept -> dept.getParentId() != null)
                .collect(Collectors.groupingBy(SystemDeptEntity::getParentId));
        Set<Long> result = new LinkedHashSet<>();
        collectDeptIds(rootDeptId, childrenByParentId, result);
        return new ArrayList<>(result);
    }

    private void collectDeptIds(
            Long deptId,
            Map<Long, List<SystemDeptEntity>> childrenByParentId,
            Set<Long> result
    ) {
        if (deptId == null || result.contains(deptId)) {
            return;
        }
        result.add(deptId);
        childrenByParentId.getOrDefault(deptId, List.of())
                .stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SystemDeptEntity::getId))
                .forEach(child -> collectDeptIds(child.getId(), childrenByParentId, result));
    }
}
