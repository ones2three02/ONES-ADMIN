package com.ones.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.system.dto.RoleResponse;
import com.ones.admin.system.dto.UserCreateRequest;
import com.ones.admin.system.dto.UserResponse;
import com.ones.admin.system.dto.UserUpdateRequest;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.entity.SystemRoleEntity;
import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.entity.SystemUserRoleEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
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
    private final SystemDeptMapper deptMapper;
    private final SystemRoleMapper roleMapper;
    private final SystemUserRoleMapper userRoleMapper;
    private final DataScopeService dataScopeService;

    public UserManagementService(
            SystemUserMapper userMapper,
            SystemDeptMapper deptMapper,
            SystemRoleMapper roleMapper,
            SystemUserRoleMapper userRoleMapper,
            DataScopeService dataScopeService
    ) {
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.dataScopeService = dataScopeService;
    }

    public List<UserResponse> listUsers() {
        DataScopeContext context = dataScopeService.currentContext();
        LambdaQueryWrapper<SystemUserEntity> wrapper = new LambdaQueryWrapper<SystemUserEntity>()
                .orderByAsc(SystemUserEntity::getId);

        if (!context.isAll()) {
            if (context.dataScope() == DataScope.SELF) {
                wrapper.eq(SystemUserEntity::getId, context.userId());
            } else if (context.dataScope() == DataScope.DEPT || context.dataScope() == DataScope.DEPT_AND_CHILD) {
                if (context.deptIds() != null && !context.deptIds().isEmpty()) {
                    wrapper.in(SystemUserEntity::getDeptId, context.deptIds());
                } else {
                    wrapper.eq(SystemUserEntity::getId, -1L); // 无部门权限则查不到任何人
                }
            }
        }

        return userMapper.selectList(wrapper)
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
        List<String> roleCodes = normalizeRoleCodes(request.roleCodes());
        assertRoleCodesNotEmpty(roleCodes);

        SystemUserEntity user = new SystemUserEntity();
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setDeptId(requireEnabledDept(request.deptId()));
        user.setRemark(normalizeNullable(request.remark()));
        user.setAvatar("https://api.dicebear.com/9.x/initials/svg?seed=" + username);
        user.setPasswordHash(PASSWORD_ENCODER.encode(request.password()));
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setFailedLoginCount(0);
        userMapper.insert(user);
        syncUserRoles(user.getId(), roleCodes);
        return toResponse(userMapper.selectById(user.getId()));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        SystemUserEntity user = getRequiredUser(id);
        List<String> roleCodes = request.roleCodes() == null
                ? roleMapper.selectRoleCodesByUserId(user.getId())
                : normalizeRoleCodes(request.roleCodes());
        assertRoleCodesNotEmpty(roleCodes);
        if ("admin".equals(user.getUsername())) {
            if (request.enabled() != null && !request.enabled()) {
                throw new BusinessException(SystemErrorCode.DEFAULT_ADMIN_CANNOT_DISABLE);
            }
            if (!roleCodes.contains("SUPER_ADMIN")) {
                throw new BusinessException(SystemErrorCode.DEFAULT_ADMIN_ROLE_REQUIRED);
            }
        }
        user.setDisplayName(request.displayName().trim());
        user.setDeptId(requireEnabledDept(request.deptId()));
        user.setRemark(normalizeNullable(request.remark()));
        user.setEnabled(request.enabled() == null || request.enabled());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(PASSWORD_ENCODER.encode(request.password()));
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        syncUserRoles(user.getId(), roleCodes);
        return toResponse(userMapper.selectById(id));
    }

    @Transactional
    public void deleteUser(Long id) {
        SystemUserEntity user = getRequiredUser(id);
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException(SystemErrorCode.DEFAULT_ADMIN_CANNOT_DELETE);
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SystemUserRoleEntity>()
                .eq(SystemUserRoleEntity::getUserId, id));
        userMapper.deleteById(id);
    }

    private void assertUsernameAvailable(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getUsername, username));
        if (count > 0) {
            throw new BusinessException(SystemErrorCode.USERNAME_EXISTS);
        }
    }

    private SystemUserEntity getRequiredUser(Long id) {
        SystemUserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
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
            throw new BusinessException(SystemErrorCode.USER_ROLE_NOT_AVAILABLE);
        }
        return roles;
    }

    private void assertRoleCodesNotEmpty(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException(SystemErrorCode.USER_ROLE_REQUIRED);
        }
    }

    private Long requireEnabledDept(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SystemDeptEntity dept = deptMapper.selectById(deptId);
        if (dept == null || !Boolean.TRUE.equals(dept.getEnabled())) {
            throw new BusinessException(SystemErrorCode.USER_DEPT_NOT_AVAILABLE);
        }
        return deptId;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
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
                user.getDeptId(),
                user.getAvatar(),
                user.getRemark(),
                Boolean.TRUE.equals(user.getEnabled()),
                user.getLastLoginAt(),
                user.getLockedUntil(),
                user.getCreatedAt(),
                roleMapper.selectRoleCodesByUserId(user.getId())
        );
    }
}
