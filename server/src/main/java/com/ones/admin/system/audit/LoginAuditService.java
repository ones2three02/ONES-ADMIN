package com.ones.admin.system.audit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.LoginLogQuery;
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

    public PageResult<LoginLogResponse> queryPage(LoginLogQuery query) {
        IPage<SystemLoginLogEntity> page = loginLogMapper.selectPage(
                query.toMyBatisPage(),
                buildQueryWrapper(query)
        );
        List<LoginLogResponse> records = page.getRecords()
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(page, records);
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

    private LambdaQueryWrapper<SystemLoginLogEntity> buildQueryWrapper(LoginLogQuery query) {
        LambdaQueryWrapper<SystemLoginLogEntity> wrapper = new LambdaQueryWrapper<SystemLoginLogEntity>()
                .orderByDesc(SystemLoginLogEntity::getId);
        if (hasText(query.getUsername())) {
            wrapper.like(SystemLoginLogEntity::getUsername, query.getUsername().trim());
        }
        if (query.getSuccess() != null) {
            wrapper.eq(SystemLoginLogEntity::getSuccess, query.getSuccess());
        }
        if (hasText(query.getIp())) {
            wrapper.like(SystemLoginLogEntity::getIp, query.getIp().trim());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(SystemLoginLogEntity::getCreatedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(SystemLoginLogEntity::getCreatedAt, query.getEndTime());
        }
        return wrapper;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
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
