package com.ones.admin.auth.oauth.repository;

import com.ones.admin.auth.oauth.model.ExternalIdentity;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface ExternalIdentityRepository {

    Optional<ExternalIdentity> findActive(String provider, String tenantKey, String externalSubject);

    Optional<ExternalIdentity> findAny(String provider, String tenantKey, String externalSubject);

    List<ExternalIdentity> findByUserId(Long userId);

    ExternalIdentity save(ExternalIdentity identity);

    void disable(Long identityId, Long userId, LocalDateTime updatedAt);

    void recordLogin(Long identityId, LocalDateTime loginAt);
}
