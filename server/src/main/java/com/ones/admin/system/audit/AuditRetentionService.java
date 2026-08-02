package com.ones.admin.system.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.system.dto.AuditRetentionCleanupResponse;
import com.ones.admin.system.dto.AuditRetentionSummaryResponse;
import com.ones.admin.system.entity.SystemLoginLogEntity;
import com.ones.admin.system.entity.SystemOperationLogEntity;
import com.ones.admin.system.mapper.SystemLoginLogMapper;
import com.ones.admin.system.mapper.SystemOperationLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditRetentionService {

    private final AuditRetentionProperties properties;
    private final SystemLoginLogMapper loginLogMapper;
    private final SystemOperationLogMapper operationLogMapper;

    public AuditRetentionService(
            AuditRetentionProperties properties,
            SystemLoginLogMapper loginLogMapper,
            SystemOperationLogMapper operationLogMapper
    ) {
        this.properties = properties;
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
    }

    public AuditRetentionSummaryResponse summarize() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime loginExpireBefore = loginExpireBefore(now);
        LocalDateTime operationExpireBefore = operationExpireBefore(now);
        return new AuditRetentionSummaryResponse(
                properties.getLoginLogDays(),
                properties.getOperationLogDays(),
                loginExpireBefore,
                operationExpireBefore,
                loginLogMapper.selectCount(loginExpiredWrapper(loginExpireBefore)),
                operationLogMapper.selectCount(operationExpiredWrapper(operationExpireBefore))
        );
    }

    @Transactional
    public AuditRetentionCleanupResponse cleanupExpiredLogs() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime loginExpireBefore = loginExpireBefore(now);
        LocalDateTime operationExpireBefore = operationExpireBefore(now);
        int deletedLoginLogs = loginLogMapper.delete(loginExpiredWrapper(loginExpireBefore));
        int deletedOperationLogs = operationLogMapper.delete(operationExpiredWrapper(operationExpireBefore));
        return new AuditRetentionCleanupResponse(
                properties.getLoginLogDays(),
                properties.getOperationLogDays(),
                loginExpireBefore,
                operationExpireBefore,
                deletedLoginLogs,
                deletedOperationLogs
        );
    }

    private LocalDateTime loginExpireBefore(LocalDateTime now) {
        return now.minusDays(properties.getLoginLogDays());
    }

    private LocalDateTime operationExpireBefore(LocalDateTime now) {
        return now.minusDays(properties.getOperationLogDays());
    }

    private LambdaQueryWrapper<SystemLoginLogEntity> loginExpiredWrapper(LocalDateTime expireBefore) {
        return new LambdaQueryWrapper<SystemLoginLogEntity>()
                .lt(SystemLoginLogEntity::getCreatedAt, expireBefore);
    }

    private LambdaQueryWrapper<SystemOperationLogEntity> operationExpiredWrapper(LocalDateTime expireBefore) {
        return new LambdaQueryWrapper<SystemOperationLogEntity>()
                .lt(SystemOperationLogEntity::getCreatedAt, expireBefore);
    }
}
