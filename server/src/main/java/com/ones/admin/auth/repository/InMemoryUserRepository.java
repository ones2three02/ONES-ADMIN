package com.ones.admin.auth.repository;

import com.ones.admin.auth.model.AdminUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InMemoryUserRepository implements UserRepository {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final Map<Long, AdminUser> usersById;
    private final Map<String, AdminUser> usersByUsername;

    public InMemoryUserRepository() {
        this(defaultUsers());
    }

    public InMemoryUserRepository(List<AdminUser> users) {
        this.usersById = users.stream().collect(Collectors.toUnmodifiableMap(AdminUser::id, Function.identity()));
        this.usersByUsername = users.stream().collect(Collectors.toUnmodifiableMap(AdminUser::username, Function.identity()));
    }

    public static InMemoryUserRepository forTest() {
        return new InMemoryUserRepository(defaultUsers());
    }

    private static List<AdminUser> defaultUsers() {
        return List.of(new AdminUser(
                1L,
                "admin",
                "系统管理员",
                "https://api.dicebear.com/9.x/initials/svg?seed=ONES",
                PASSWORD_ENCODER.encode("admin123"),
                List.of("SUPER_ADMIN"),
                List.of(
                        "dashboard:view",
                        "system:user:list",
                        "system:user:create",
                        "system:user:update",
                        "system:user:delete",
                        "system:role:list",
                        "system:role:create",
                        "system:role:update",
                        "system:role:delete",
                        "system:menu:list",
                        "system:menu:create",
                        "system:menu:update",
                        "system:menu:delete",
                        "system:dept:list",
                        "system:dept:create",
                        "system:dept:update",
                        "system:dept:delete"
                ),
                true,
                0,
                null
        ));
    }

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    public Optional<AdminUser> findById(Long id) {
        return Optional.ofNullable(usersById.get(id));
    }
}
