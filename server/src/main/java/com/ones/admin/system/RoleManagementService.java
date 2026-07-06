package com.ones.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.system.dto.RoleManagementResponse;
import com.ones.admin.system.dto.RoleSaveRequest;
import com.ones.admin.system.entity.SystemMenuEntity;
import com.ones.admin.system.entity.SystemPermissionEntity;
import com.ones.admin.system.entity.SystemRoleEntity;
import com.ones.admin.system.entity.SystemRolePermissionEntity;
import com.ones.admin.system.entity.SystemUserRoleEntity;
import com.ones.admin.system.mapper.SystemMenuMapper;
import com.ones.admin.system.mapper.SystemPermissionMapper;
import com.ones.admin.system.mapper.SystemRoleMapper;
import com.ones.admin.system.mapper.SystemRolePermissionMapper;
import com.ones.admin.system.mapper.SystemUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class RoleManagementService {

    private final SystemRoleMapper roleMapper;
    private final SystemMenuMapper menuMapper;
    private final SystemPermissionMapper permissionMapper;
    private final SystemRolePermissionMapper rolePermissionMapper;
    private final SystemUserRoleMapper userRoleMapper;

    public RoleManagementService(
            SystemRoleMapper roleMapper,
            SystemMenuMapper menuMapper,
            SystemPermissionMapper permissionMapper,
            SystemRolePermissionMapper rolePermissionMapper,
            SystemUserRoleMapper userRoleMapper
    ) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public List<RoleManagementResponse> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SystemRoleEntity>()
                        .orderByAsc(SystemRoleEntity::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoleManagementResponse create(RoleSaveRequest request) {
        String code = requireCode(request.code());
        String name = requireName(request.name());
        assertCodeAvailable(code, null);

        SystemRoleEntity role = new SystemRoleEntity();
        role.setCode(code);
        role.setName(name);
        role.setDataScope(normalizeDataScope(request.dataScope(), DataScope.DEPT_AND_CHILD).name());
        role.setRemark(normalizeNullable(request.remark()));
        role.setEnabled(toEnabled(request.status()));
        roleMapper.insert(role);
        syncRolePermissions(role.getId(), request.permissions());
        return toResponse(roleMapper.selectById(role.getId()));
    }

    @Transactional
    public RoleManagementResponse update(Long id, RoleSaveRequest request) {
        SystemRoleEntity role = getRequiredRole(id);
        boolean superAdmin = "SUPER_ADMIN".equals(role.getCode());
        if (request.code() != null && !request.code().trim().isBlank()) {
            String code = normalizeCode(request.code());
            if (superAdmin && !"SUPER_ADMIN".equals(code)) {
                throw new BusinessException(SystemErrorCode.SUPER_ADMIN_CODE_LOCKED);
            }
            assertCodeAvailable(code, id);
            role.setCode(code);
        }
        if (request.name() != null && !request.name().trim().isBlank()) {
            role.setName(request.name().trim());
        }
        if (request.dataScope() != null && !request.dataScope().trim().isBlank()) {
            DataScope dataScope = normalizeDataScope(request.dataScope(), DataScope.DEPT_AND_CHILD);
            if (superAdmin && dataScope != DataScope.ALL) {
                throw new BusinessException(SystemErrorCode.SUPER_ADMIN_PERMISSION_LOCKED);
            }
            role.setDataScope(dataScope.name());
        }
        if (request.remark() != null) {
            role.setRemark(normalizeNullable(request.remark()));
        }
        if (request.status() != null) {
            if ("SUPER_ADMIN".equals(role.getCode()) && request.status() == 0) {
                throw new BusinessException(SystemErrorCode.SUPER_ADMIN_CANNOT_DISABLE);
            }
            role.setEnabled(request.status() == 1);
        }
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);
        if (request.permissions() != null) {
            if (superAdmin) {
                throw new BusinessException(SystemErrorCode.SUPER_ADMIN_PERMISSION_LOCKED);
            }
            syncRolePermissions(role.getId(), request.permissions());
        }
        return toResponse(roleMapper.selectById(id));
    }

    @Transactional
    public void delete(Long id) {
        SystemRoleEntity role = getRequiredRole(id);
        if ("SUPER_ADMIN".equals(role.getCode())) {
            throw new BusinessException(SystemErrorCode.SUPER_ADMIN_CANNOT_DELETE);
        }
        Long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<SystemUserRoleEntity>()
                .eq(SystemUserRoleEntity::getRoleId, id));
        if (userCount > 0) {
            throw new BusinessException(SystemErrorCode.ROLE_ASSIGNED_TO_USER);
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SystemRolePermissionEntity>()
                .eq(SystemRolePermissionEntity::getRoleId, id));
        roleMapper.deleteById(id);
    }

    private void syncRolePermissions(Long roleId, List<String> menuIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<SystemRolePermissionEntity>()
                .eq(SystemRolePermissionEntity::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<Long> normalizedMenuIds = menuIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .distinct()
                .toList();
        if (normalizedMenuIds.isEmpty()) {
            return;
        }
        List<SystemMenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<SystemMenuEntity>()
                .in(SystemMenuEntity::getId, normalizedMenuIds));
        if (menus.size() != normalizedMenuIds.size()) {
            throw new BusinessException(SystemErrorCode.ROLE_MENU_PERMISSION_NOT_FOUND);
        }
        Set<String> insertedCodes = new HashSet<>();
        for (SystemMenuEntity menu : menus) {
            if (menu.getAuthCode() == null || menu.getAuthCode().isBlank()) {
                continue;
            }
            if (!insertedCodes.add(menu.getAuthCode())) {
                continue;
            }
            SystemPermissionEntity permission = ensurePermission(menu.getAuthCode(), menu.getTitle());
            SystemRolePermissionEntity relation = new SystemRolePermissionEntity();
            relation.setRoleId(roleId);
            relation.setPermissionId(permission.getId());
            rolePermissionMapper.insert(relation);
        }
    }

    private SystemPermissionEntity ensurePermission(String code, String name) {
        SystemPermissionEntity existing = permissionMapper.selectOne(new LambdaQueryWrapper<SystemPermissionEntity>()
                .eq(SystemPermissionEntity::getCode, code)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        SystemPermissionEntity permission = new SystemPermissionEntity();
        permission.setCode(code);
        permission.setName(name);
        permissionMapper.insert(permission);
        return permission;
    }

    private SystemRoleEntity getRequiredRole(Long id) {
        SystemRoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(SystemErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    private void assertCodeAvailable(String code, Long exceptId) {
        LambdaQueryWrapper<SystemRoleEntity> wrapper = new LambdaQueryWrapper<SystemRoleEntity>()
                .eq(SystemRoleEntity::getCode, code);
        if (exceptId != null) {
            wrapper.ne(SystemRoleEntity::getId, exceptId);
        }
        Long count = roleMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(SystemErrorCode.ROLE_CODE_EXISTS);
        }
    }

    private String requireCode(String code) {
        if (code == null || code.trim().isBlank()) {
            throw new BusinessException(SystemErrorCode.ROLE_CODE_REQUIRED);
        }
        return normalizeCode(code);
    }

    private String requireName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new BusinessException(SystemErrorCode.ROLE_NAME_REQUIRED);
        }
        return name.trim();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean toEnabled(Integer status) {
        return status == null || status == 1;
    }

    private RoleManagementResponse toResponse(SystemRoleEntity role) {
        return new RoleManagementResponse(
                String.valueOf(role.getId()),
                role.getCode(),
                role.getName(),
                normalizeDataScope(role.getDataScope(), defaultDataScope(role)).name(),
                role.getRemark(),
                Boolean.TRUE.equals(role.getEnabled()) ? 1 : 0,
                role.getCreatedAt(),
                menuMapper.selectMenuIdsByRoleId(role.getId()).stream()
                        .map(String::valueOf)
                        .toList()
        );
    }

    private DataScope defaultDataScope(SystemRoleEntity role) {
        return "SUPER_ADMIN".equals(role.getCode()) ? DataScope.ALL : DataScope.DEPT_AND_CHILD;
    }

    private DataScope normalizeDataScope(String dataScope, DataScope defaultScope) {
        try {
            return dataScope == null || dataScope.trim().isBlank()
                    ? defaultScope
                    : DataScope.from(dataScope);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(SystemErrorCode.ROLE_DATA_SCOPE_INVALID);
        }
    }
}
