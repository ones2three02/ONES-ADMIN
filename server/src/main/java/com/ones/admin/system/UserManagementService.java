package com.ones.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.system.dto.RoleResponse;
import com.ones.admin.system.dto.UserCreateRequest;
import com.ones.admin.system.dto.UserResponse;
import com.ones.admin.system.dto.UserUpdateRequest;
import com.ones.admin.system.entity.SystemRoleEntity;
import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.entity.SystemUserRoleEntity;
import com.ones.admin.system.mapper.SystemRoleMapper;
import com.ones.admin.system.mapper.SystemUserMapper;
import com.ones.admin.system.mapper.SystemUserRoleMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UserManagementService {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final SystemUserMapper userMapper;
    private final SystemRoleMapper roleMapper;
    private final SystemUserRoleMapper userRoleMapper;

    public UserManagementService(
            SystemUserMapper userMapper,
            SystemRoleMapper roleMapper,
            SystemUserRoleMapper userRoleMapper
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public List<UserResponse> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<SystemUserEntity>()
                        .orderByAsc(SystemUserEntity::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RoleResponse> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SystemRoleEntity>()
                        .orderByAsc(SystemRoleEntity::getId))
                .stream()
                .map(role -> new RoleResponse(
                        role.getId(),
                        role.getCode(),
                        role.getName(),
                        Boolean.TRUE.equals(role.getEnabled())
                ))
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String username = request.username().trim();
        assertUsernameAvailable(username);

        SystemUserEntity user = new SystemUserEntity();
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setAvatar("https://api.dicebear.com/9.x/initials/svg?seed=" + username);
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.password()));
        user.setEnabled(request.enabled() == null || request.enabled());
        userMapper.insert(user);
        syncUserRoles(user.getId(), request.roleCodes());
        return toResponse(userMapper.selectById(user.getId()));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        SystemUserEntity user = getRequiredUser(id);
        user.setDisplayName(request.displayName().trim());
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        syncUserRoles(user.getId(), request.roleCodes());
        return toResponse(userMapper.selectById(id));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (id == 1L) {
            throw new BusinessException("默认管理员不能删除");
        }
        getRequiredUser(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SystemUserRoleEntity>()
                .eq(SystemUserRoleEntity::getUserId, id));
        userMapper.deleteById(id);
    }

    private void assertUsernameAvailable(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getUsername, username));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
    }

    private SystemUserEntity getRequiredUser(Long id) {
        SystemUserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private void syncUserRoles(Long userId, List<String> roleCodes) {
        userRoleMapper.delete(new LambdaQueryWrapper<SystemUserRoleEntity>()
                .eq(SystemUserRoleEntity::getUserId, userId));

        List<SystemRoleEntity> roles = findEnabledRoles(roleCodes);
        for (SystemRoleEntity role : roles) {
            SystemUserRoleEntity relation = new SystemUserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            userRoleMapper.insert(relation);
        }
    }

    private List<SystemRoleEntity> findEnabledRoles(List<String> roleCodes) {
        List<String> normalizedCodes = normalizeRoleCodes(roleCodes);
        if (normalizedCodes.isEmpty()) {
            return List.of();
        }
        List<SystemRoleEntity> roles = roleMapper.selectList(new LambdaQueryWrapper<SystemRoleEntity>()
                .in(SystemRoleEntity::getCode, normalizedCodes)
                .eq(SystemRoleEntity::getEnabled, true));
        if (roles.size() != normalizedCodes.size()) {
            throw new BusinessException("角色不存在或已停用");
        }
        return roles;
    }

    private List<String> normalizeRoleCodes(List<String> roleCodes) {
        if (roleCodes == null) {
            return List.of();
        }
        return roleCodes.stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
    }

    private UserResponse toResponse(SystemUserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatar(),
                Boolean.TRUE.equals(user.getEnabled()),
                roleMapper.selectRoleCodesByUserId(user.getId())
        );
    }
}
