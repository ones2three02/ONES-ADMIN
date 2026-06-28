package com.ones.admin.system.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.auth.model.AdminUser;
import com.ones.admin.auth.repository.UserRepository;
import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.mapper.SystemPermissionMapper;
import com.ones.admin.system.mapper.SystemRoleMapper;
import com.ones.admin.system.mapper.SystemUserMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisUserRepository implements UserRepository {

    private final SystemUserMapper userMapper;
    private final SystemRoleMapper roleMapper;
    private final SystemPermissionMapper permissionMapper;

    public MyBatisUserRepository(
            SystemUserMapper userMapper,
            SystemRoleMapper roleMapper,
            SystemPermissionMapper permissionMapper
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        SystemUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getUsername, username)
                .last("limit 1"));
        return Optional.ofNullable(user).map(this::toAdminUser);
    }

    @Override
    public Optional<AdminUser> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(this::toAdminUser);
    }

    @Override
    public void recordLoginSuccess(Long userId) {
        SystemUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userMapper.updateById(user);
    }

    @Override
    public void recordLoginFailure(Long userId, int failedLoginCount, LocalDateTime lockedUntil) {
        SystemUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        user.setFailedLoginCount(failedLoginCount);
        user.setLockedUntil(lockedUntil);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private AdminUser toAdminUser(SystemUserEntity user) {
        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = permissionMapper.selectPermissionCodesByUserId(user.getId());
        return new AdminUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatar(),
                user.getPasswordHash(),
                roles,
                permissions,
                Boolean.TRUE.equals(user.getEnabled()),
                user.getFailedLoginCount(),
                user.getLockedUntil()
        );
    }
}
