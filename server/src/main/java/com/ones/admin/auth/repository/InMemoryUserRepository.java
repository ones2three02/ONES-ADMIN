package com.ones.admin.auth.repository;

import com.ones.admin.auth.model.AdminUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
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
                        "system:role:list",
                        "system:menu:list"
                ),
                true
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
