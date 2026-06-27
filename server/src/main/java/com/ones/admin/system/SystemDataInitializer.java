package com.ones.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.system.entity.SystemPermissionEntity;
import com.ones.admin.system.entity.SystemRoleEntity;
import com.ones.admin.system.entity.SystemRolePermissionEntity;
import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.entity.SystemUserRoleEntity;
import com.ones.admin.system.mapper.SystemPermissionMapper;
import com.ones.admin.system.mapper.SystemRoleMapper;
import com.ones.admin.system.mapper.SystemRolePermissionMapper;
import com.ones.admin.system.mapper.SystemUserMapper;
import com.ones.admin.system.mapper.SystemUserRoleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SystemDataInitializer implements ApplicationRunner {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final SystemUserMapper userMapper;
    private final SystemRoleMapper roleMapper;
    private final SystemPermissionMapper permissionMapper;
    private final SystemUserRoleMapper userRoleMapper;
    private final SystemRolePermissionMapper rolePermissionMapper;

    public SystemDataInitializer(
            SystemUserMapper userMapper,
            SystemRoleMapper roleMapper,
            SystemPermissionMapper permissionMapper,
            SystemUserRoleMapper userRoleMapper,
            SystemRolePermissionMapper rolePermissionMapper
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        SystemRoleEntity superAdminRole = ensureRole("SUPER_ADMIN", "超级管理员");
        SystemRoleEntity operatorRole = ensureRole("OPERATOR", "运营人员");

        List<SystemPermissionEntity> permissions = List.of(
                ensurePermission("dashboard:view", "工作台查看"),
                ensurePermission("system:user:list", "用户查询"),
                ensurePermission("system:user:create", "用户新增"),
                ensurePermission("system:user:update", "用户编辑"),
                ensurePermission("system:user:delete", "用户删除"),
                ensurePermission("system:role:list", "角色查询"),
                ensurePermission("system:menu:list", "菜单查询")
        );

        permissions.forEach(permission -> ensureRolePermission(superAdminRole.getId(), permission.getId()));
        ensureRolePermission(operatorRole.getId(), permissionIdByCode(permissions, "dashboard:view"));
        ensureRolePermission(operatorRole.getId(), permissionIdByCode(permissions, "system:user:list"));

        SystemUserEntity admin = ensureAdminUser();
        ensureUserRole(admin.getId(), superAdminRole.getId());
    }

    private SystemRoleEntity ensureRole(String code, String name) {
        SystemRoleEntity existing = roleMapper.selectOne(new LambdaQueryWrapper<SystemRoleEntity>()
                .eq(SystemRoleEntity::getCode, code)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        SystemRoleEntity role = new SystemRoleEntity();
        role.setCode(code);
        role.setName(name);
        role.setEnabled(true);
        roleMapper.insert(role);
        return role;
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

    private SystemUserEntity ensureAdminUser() {
        SystemUserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getUsername, "admin")
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        SystemUserEntity user = new SystemUserEntity();
        user.setUsername("admin");
        user.setDisplayName("系统管理员");
        user.setAvatar("https://api.dicebear.com/9.x/initials/svg?seed=ONES");
        user.setPasswordHash(PASSWORD_ENCODER.encode("admin123"));
        user.setEnabled(true);
        userMapper.insert(user);
        return user;
    }

    private void ensureUserRole(Long userId, Long roleId) {
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SystemUserRoleEntity>()
                .eq(SystemUserRoleEntity::getUserId, userId)
                .eq(SystemUserRoleEntity::getRoleId, roleId));
        if (count > 0) {
            return;
        }
        SystemUserRoleEntity relation = new SystemUserRoleEntity();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        userRoleMapper.insert(relation);
    }

    private void ensureRolePermission(Long roleId, Long permissionId) {
        Long count = rolePermissionMapper.selectCount(new LambdaQueryWrapper<SystemRolePermissionEntity>()
                .eq(SystemRolePermissionEntity::getRoleId, roleId)
                .eq(SystemRolePermissionEntity::getPermissionId, permissionId));
        if (count > 0) {
            return;
        }
        SystemRolePermissionEntity relation = new SystemRolePermissionEntity();
        relation.setRoleId(roleId);
        relation.setPermissionId(permissionId);
        rolePermissionMapper.insert(relation);
    }

    private Long permissionIdByCode(List<SystemPermissionEntity> permissions, String code) {
        Map<String, Long> permissionIds = permissions.stream()
                .collect(java.util.stream.Collectors.toMap(SystemPermissionEntity::getCode, SystemPermissionEntity::getId));
        return permissionIds.get(code);
    }
}
