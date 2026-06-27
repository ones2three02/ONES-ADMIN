package com.ones.admin.auth.repository;

import com.ones.admin.auth.model.AdminUser;

import java.util.Optional;

public interface UserRepository {

    Optional<AdminUser> findByUsername(String username);

    Optional<AdminUser> findById(Long id);
}
