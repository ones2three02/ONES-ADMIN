package com.ones.admin.auth.oauth.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.auth.oauth.ExternalIdentityStatus;
import com.ones.admin.auth.oauth.entity.SystemExternalIdentityEntity;
import com.ones.admin.auth.oauth.mapper.SystemExternalIdentityMapper;
import com.ones.admin.auth.oauth.model.ExternalIdentity;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public class MyBatisExternalIdentityRepository implements ExternalIdentityRepository {

    private final SystemExternalIdentityMapper mapper;

    public MyBatisExternalIdentityRepository(SystemExternalIdentityMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ExternalIdentity> findActive(String provider, String tenantKey, String externalSubject) {
        return find(provider, tenantKey, externalSubject, true);
    }

    @Override
    public Optional<ExternalIdentity> findAny(String provider, String tenantKey, String externalSubject) {
        return find(provider, tenantKey, externalSubject, false);
    }

    @Override
    public List<ExternalIdentity> findByUserId(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<SystemExternalIdentityEntity>()
                        .eq(SystemExternalIdentityEntity::getUserId, userId)
                        .orderByAsc(SystemExternalIdentityEntity::getProvider))
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public ExternalIdentity save(ExternalIdentity identity) {
        LocalDateTime now = LocalDateTime.now();
        SystemExternalIdentityEntity entity = new SystemExternalIdentityEntity();
        entity.setId(identity.id());
        entity.setProvider(identity.provider());
        entity.setTenantKey(identity.tenantKey());
        entity.setExternalSubject(identity.externalSubject());
        entity.setUserId(identity.userId());
        entity.setStatus(identity.status().name());
        entity.setLastLoginAt(identity.lastLoginAt());
        entity.setUpdatedAt(now);
        if (identity.id() == null) {
            entity.setCreatedAt(now);
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toModel(entity);
    }

    @Override
    public void disable(Long identityId, Long userId, LocalDateTime updatedAt) {
        SystemExternalIdentityEntity existing = mapper.selectOne(
                new LambdaQueryWrapper<SystemExternalIdentityEntity>()
                        .eq(SystemExternalIdentityEntity::getId, identityId)
                        .eq(SystemExternalIdentityEntity::getUserId, userId)
                        .last("limit 1")
        );
        if (existing == null) {
            return;
        }
        existing.setStatus(ExternalIdentityStatus.DISABLED.name());
        existing.setUpdatedAt(updatedAt);
        mapper.updateById(existing);
    }

    @Override
    public void recordLogin(Long identityId, LocalDateTime loginAt) {
        SystemExternalIdentityEntity entity = new SystemExternalIdentityEntity();
        entity.setId(identityId);
        entity.setLastLoginAt(loginAt);
        entity.setUpdatedAt(loginAt);
        mapper.updateById(entity);
    }

    private Optional<ExternalIdentity> find(
            String provider,
            String tenantKey,
            String externalSubject,
            boolean activeOnly
    ) {
        LambdaQueryWrapper<SystemExternalIdentityEntity> wrapper =
                new LambdaQueryWrapper<SystemExternalIdentityEntity>()
                        .eq(SystemExternalIdentityEntity::getProvider, provider)
                        .eq(SystemExternalIdentityEntity::getTenantKey, tenantKey)
                        .eq(SystemExternalIdentityEntity::getExternalSubject, externalSubject);
        if (activeOnly) {
            wrapper.eq(SystemExternalIdentityEntity::getStatus, ExternalIdentityStatus.ACTIVE.name());
        }
        return Optional.ofNullable(mapper.selectOne(wrapper.last("limit 1"))).map(this::toModel);
    }

    private ExternalIdentity toModel(SystemExternalIdentityEntity entity) {
        return new ExternalIdentity(
                entity.getId(),
                entity.getProvider(),
                entity.getTenantKey(),
                entity.getExternalSubject(),
                entity.getUserId(),
                ExternalIdentityStatus.valueOf(entity.getStatus()),
                entity.getLastLoginAt()
        );
    }
}
