package com.ones.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.system.dto.DeptResponse;
import com.ones.admin.system.dto.DeptSaveRequest;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
import com.ones.admin.system.mapper.SystemUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DeptManagementService {

    private final SystemDeptMapper deptMapper;
    private final SystemUserMapper userMapper;

    public DeptManagementService(SystemDeptMapper deptMapper, SystemUserMapper userMapper) {
        this.deptMapper = deptMapper;
        this.userMapper = userMapper;
    }

    public List<DeptResponse> listTree() {
        List<SystemDeptEntity> depts = deptMapper.selectList(new LambdaQueryWrapper<SystemDeptEntity>()
                .orderByAsc(SystemDeptEntity::getId));
        Map<Long, List<SystemDeptEntity>> childrenByParentId = depts.stream()
                .filter(dept -> dept.getParentId() != null)
                .collect(Collectors.groupingBy(SystemDeptEntity::getParentId));
        return depts.stream()
                .filter(dept -> dept.getParentId() == null)
                .sorted(Comparator.comparing(SystemDeptEntity::getId))
                .map(dept -> toResponse(dept, childrenByParentId))
                .toList();
    }

    @Transactional
    public DeptResponse create(DeptSaveRequest request) {
        Long parentId = normalizePid(request.pid());
        assertParentExists(parentId);

        SystemDeptEntity dept = new SystemDeptEntity();
        dept.setParentId(parentId);
        dept.setName(request.name().trim());
        dept.setRemark(normalizeNullable(request.remark()));
        dept.setEnabled(toEnabled(request.status()));
        deptMapper.insert(dept);
        return toResponse(deptMapper.selectById(dept.getId()), Map.of());
    }

    @Transactional
    public DeptResponse update(Long id, DeptSaveRequest request) {
        SystemDeptEntity dept = getRequiredDept(id);
        Long parentId = normalizePid(request.pid());
        assertParentExists(parentId);
        assertParentNotDescendant(id, parentId);
        dept.setParentId(parentId);
        dept.setName(request.name().trim());
        dept.setRemark(normalizeNullable(request.remark()));
        dept.setEnabled(toEnabled(request.status()));
        dept.setUpdatedAt(LocalDateTime.now());
        deptMapper.updateById(dept);
        return toResponse(deptMapper.selectById(id), Map.of());
    }

    @Transactional
    public void delete(Long id) {
        getRequiredDept(id);
        Long childCount = deptMapper.selectCount(new LambdaQueryWrapper<SystemDeptEntity>()
                .eq(SystemDeptEntity::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("存在下级部门，不能删除");
        }
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException("部门下存在用户，不能删除");
        }
        deptMapper.deleteById(id);
    }

    private SystemDeptEntity getRequiredDept(Long id) {
        SystemDeptEntity dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(404, "部门不存在");
        }
        return dept;
    }

    private void assertParentExists(Long parentId) {
        if (parentId == null) {
            return;
        }
        if (deptMapper.selectById(parentId) == null) {
            throw new BusinessException("上级部门不存在");
        }
    }

    private void assertParentNotDescendant(Long currentId, Long parentId) {
        Long cursor = parentId;
        while (cursor != null) {
            if (Objects.equals(cursor, currentId)) {
                throw new BusinessException("上级部门不能选择自己或自己的下级");
            }
            SystemDeptEntity parent = deptMapper.selectById(cursor);
            if (parent == null) {
                throw new BusinessException("上级部门不存在");
            }
            cursor = parent.getParentId();
        }
    }

    private Long normalizePid(Long pid) {
        if (pid == null || pid == 0L) {
            return null;
        }
        return pid;
    }

    private boolean toEnabled(Integer status) {
        return status == null || status == 1;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private DeptResponse toResponse(SystemDeptEntity dept, Map<Long, List<SystemDeptEntity>> childrenByParentId) {
        List<DeptResponse> children = childrenByParentId
                .getOrDefault(dept.getId(), new ArrayList<>())
                .stream()
                .sorted(Comparator.comparing(SystemDeptEntity::getId))
                .map(child -> toResponse(child, childrenByParentId))
                .toList();
        return new DeptResponse(
                String.valueOf(dept.getId()),
                dept.getParentId() == null ? "0" : String.valueOf(dept.getParentId()),
                dept.getName(),
                dept.getRemark(),
                Boolean.TRUE.equals(dept.getEnabled()) ? 1 : 0,
                dept.getCreatedAt(),
                children
        );
    }
}
