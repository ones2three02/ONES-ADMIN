package com.ones.admin.auth;

import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.auth.dto.LoginResponse;
import com.ones.admin.auth.dto.UserProfile;
import com.ones.admin.auth.model.AdminUser;
import com.ones.admin.auth.repository.InMemoryUserRepository;
import com.ones.admin.auth.repository.UserRepository;
import com.ones.admin.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this(userRepository, new BCryptPasswordEncoder());
    }

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static AuthService createForTest() {
        return new AuthService(InMemoryUserRepository.forTest(), new BCryptPasswordEncoder());
    }

    public LoginResponse login(LoginRequest request) {
        AdminUser user = userRepository.findByUsername(request.username())
                .filter(AdminUser::enabled)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.passwordHash()))
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        return new LoginResponse(null, toProfile(user), user.roles(), user.permissions());
    }

    public UserProfile getRequiredProfile(Long userId) {
        return userRepository.findById(userId)
                .filter(AdminUser::enabled)
                .map(this::toProfile)
                .orElseThrow(() -> new BusinessException(404, "用户不存在或已停用"));
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
}
