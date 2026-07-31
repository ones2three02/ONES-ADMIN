package com.ones.admin.system.audit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.event.SystemEventPayload;
import com.ones.admin.common.event.SystemEventPublisher;
import com.ones.admin.common.web.CsvExportUtils;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.LoginLogQuery;
import com.ones.admin.system.dto.LoginLogResponse;
import com.ones.admin.system.entity.SystemLoginLogEntity;
import com.ones.admin.system.mapper.SystemLoginLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoginAuditService {

    private static final String CSV_HEADER = "id,username,userId,success,failureReason,ip,userAgent,authMethod,provider,externalIdentityId,traceId,createdAt";

    private final SystemLoginLogMapper loginLogMapper;
    private final SystemEventPublisher eventPublisher;

    public LoginAuditService(SystemLoginLogMapper loginLogMapper, SystemEventPublisher eventPublisher) {
        this.loginLogMapper = loginLogMapper;
        this.eventPublisher = eventPublisher;
    }

    public void recordSuccess(String username, Long userId, AuditRequestContext context) {
        record(username, userId, true, null, "PASSWORD", null, null, context);
    }

    public void recordFailure(String username, String failureReason, AuditRequestContext context) {
        record(username, null, false, failureReason, "PASSWORD", null, null, context);
    }

    public void recordOAuthSuccess(
            String username,
            Long userId,
            String provider,
            Long externalIdentityId,
            AuditRequestContext context
    ) {
        record(username, userId, true, null, "OAUTH", provider, externalIdentityId, context);
    }

    public void recordOAuthFailure(String provider, String failureReason, AuditRequestContext context) {
        record(provider, null, false, failureReason, "OAUTH", provider, null, context);
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

    public String exportCsv(LoginLogQuery query) {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        loginLogMapper.selectList(buildQueryWrapper(query))
                .forEach(log -> csv.append(CsvExportUtils.row(
                        log.getId(),
                        log.getUsername(),
                        log.getUserId(),
                        Boolean.TRUE.equals(log.getSuccess()),
                        log.getFailureReason(),
                        log.getIp(),
                        log.getUserAgent(),
                        log.getAuthMethod(),
                        log.getProvider(),
                        log.getExternalIdentityId(),
                        log.getTraceId(),
                        log.getCreatedAt()
                )).append('\n'));
        return csv.toString();
    }

    private void record(
            String username,
            Long userId,
            boolean success,
            String failureReason,
            String authMethod,
            String provider,
            Long externalIdentityId,
            AuditRequestContext context
    ) {
        SystemLoginLogEntity log = new SystemLoginLogEntity();
        log.setUsername(AuditRequestContext.truncate(username == null ? "" : username.trim(), 64));
        log.setUserId(userId);
        log.setSuccess(success);
        log.setFailureReason(AuditRequestContext.truncate(failureReason, 255));
        log.setIp(context.ip());
        log.setUserAgent(context.userAgent());
        log.setAuthMethod(authMethod);
        log.setProvider(provider);
        log.setExternalIdentityId(externalIdentityId);
        log.setTraceId(context.traceId());
        log.setCreatedAt(LocalDateTime.now());
        loginLogMapper.insert(log);
        eventPublisher.publish(SystemEventPayload.of(
                "audit.login.recorded",
                "ones.audit.login.recorded",
                context.traceId(),
                loginAuditPayload(log)
        ));
    }

    private Map<String, Object> loginAuditPayload(SystemLoginLogEntity log) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", log.getUsername());
        payload.put("userId", log.getUserId());
        payload.put("success", log.getSuccess());
        payload.put("failureReason", log.getFailureReason());
        payload.put("ip", log.getIp());
        payload.put("userAgent", log.getUserAgent());
        payload.put("authMethod", log.getAuthMethod());
        payload.put("provider", log.getProvider());
        payload.put("externalIdentityId", log.getExternalIdentityId());
        return payload;
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
                log.getAuthMethod(),
                log.getProvider(),
                log.getExternalIdentityId(),
                log.getTraceId(),
                log.getCreatedAt()
        );
    }
}
