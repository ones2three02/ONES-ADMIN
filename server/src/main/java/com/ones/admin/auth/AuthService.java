package com.ones.admin.auth;

import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.auth.dto.LoginResponse;
import com.ones.admin.auth.dto.UserProfile;
import com.ones.admin.auth.model.AdminUser;
import com.ones.admin.auth.repository.UserRepository;
import com.ones.admin.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSecurityProperties securityProperties;

    @Autowired
    public AuthService(UserRepository userRepository, AuthSecurityProperties securityProperties) {
        this(userRepository, new BCryptPasswordEncoder(), securityProperties);
    }

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, new AuthSecurityProperties());
    }

    AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthSecurityProperties securityProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    public LoginResponse login(LoginRequest request) {
        Optional<AdminUser> userOptional = userRepository.findByUsername(request.username().trim())
                .filter(AdminUser::enabled);
        if (userOptional.isEmpty()) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        AdminUser user = userOptional.get();
        LocalDateTime now = LocalDateTime.now();
        if (user.lockedUntil() != null && user.lockedUntil().isAfter(now)) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            recordLoginFailure(user, now);
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        userRepository.recordLoginSuccess(user.id());
        return new LoginResponse(null, toProfile(user), user.roles(), user.permissions());
    }

    public LoginResponse loginByUserId(Long userId) {
        AdminUser user = userRepository.findById(userId)
                .filter(AdminUser::enabled)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_AVAILABLE));
        if (user.lockedUntil() != null && user.lockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
        }
        userRepository.recordLoginSuccess(user.id());
        return new LoginResponse(null, toProfile(user), user.roles(), user.permissions());
    }

    public UserProfile getRequiredProfile(Long userId) {
        return userRepository.findById(userId)
                .filter(AdminUser::enabled)
                .map(this::toProfile)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_AVAILABLE));
    }

    public List<String> getRoleList(Long userId) {
        return userRepository.findById(userId)
                .map(AdminUser::roles)
                .orElse(List.of());
    }

    public List<String> getPermissionList(Long userId) {
        return userRepository.findById(userId)
                .map(AdminUser::permissions)
                .orElse(List.of());
    }

    private UserProfile toProfile(AdminUser user) {
        return new UserProfile(user.id(), user.username(), user.displayName(), user.avatar());
    }

    private void recordLoginFailure(AdminUser user, LocalDateTime now) {
        LocalDateTime lockedUntil = now.plus(securityProperties.getLockDuration());
        userRepository.recordLoginFailure(user.id(), securityProperties.getMaxFailedLoginCount(), lockedUntil);
    }
}
