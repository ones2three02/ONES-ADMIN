package com.ones.admin.system.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.system.dto.LoginLogResponse;
import com.ones.admin.system.entity.SystemLoginLogEntity;
import com.ones.admin.system.mapper.SystemLoginLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginAuditService {

    private final SystemLoginLogMapper loginLogMapper;

    public LoginAuditService(SystemLoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    public void recordSuccess(String username, Long userId, AuditRequestContext context) {
        record(username, userId, true, null, context);
    }

    public void recordFailure(String username, String failureReason, AuditRequestContext context) {
        record(username, null, false, failureReason, context);
    }

    public List<LoginLogResponse> listLatest(int limit) {
        int safeLimit = clampLimit(limit);
        return loginLogMapper.selectList(new LambdaQueryWrapper<SystemLoginLogEntity>()
                        .orderByDesc(SystemLoginLogEntity::getId)
                        .last("limit " + safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void record(
            String username,
            Long userId,
            boolean success,
            String failureReason,
            AuditRequestContext context
    ) {
        SystemLoginLogEntity log = new SystemLoginLogEntity();
        log.setUsername(AuditRequestContext.truncate(username == null ? "" : username.trim(), 64));
        log.setUserId(userId);
        log.setSuccess(success);
        log.setFailureReason(AuditRequestContext.truncate(failureReason, 255));
        log.setIp(context.ip());
        log.setUserAgent(context.userAgent());
        log.setTraceId(context.traceId());
        log.setCreatedAt(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 100;
        }
        return Math.min(limit, 200);
    }

    private LoginLogResponse toResponse(SystemLoginLogEntity log) {
        return new LoginLogResponse(
                log.getId(),
                log.getUsername(),
                log.getUserId(),
                Boolean.TRUE.equals(log.getSuccess()),
                log.getFailureReason(),
                log.getIp(),
                log.getUserAgent(),
                log.getTraceId(),
                log.getCreatedAt()
        );
    }
}
