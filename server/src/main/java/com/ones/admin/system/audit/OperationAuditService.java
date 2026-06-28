package com.ones.admin.system.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.system.dto.OperationLogResponse;
import com.ones.admin.system.entity.SystemOperationLogEntity;
import com.ones.admin.system.mapper.SystemOperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationAuditService {

    private final SystemOperationLogMapper operationLogMapper;

    public OperationAuditService(SystemOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void record(OperationAuditRecord record) {
        SystemOperationLogEntity log = new SystemOperationLogEntity();
        log.setUserId(record.userId());
        log.setMethod(AuditRequestContext.truncate(record.method(), 16));
        log.setPath(AuditRequestContext.truncate(record.path(), 255));
        log.setModule(AuditRequestContext.truncate(record.module(), 128));
        log.setOperation(AuditRequestContext.truncate(record.operation(), 128));
        log.setPermissionCode(AuditRequestContext.truncate(record.permissionCode(), 255));
        log.setSuccess(record.success());
        log.setResponseCode(record.responseCode());
        log.setErrorMessage(AuditRequestContext.truncate(record.errorMessage(), 512));
        log.setTraceId(record.traceId());
        log.setIp(record.ip());
        log.setUserAgent(record.userAgent());
        log.setDurationMs(record.durationMs());
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    public List<OperationLogResponse> listLatest(int limit) {
        int safeLimit = clampLimit(limit);
        return operationLogMapper.selectList(new LambdaQueryWrapper<SystemOperationLogEntity>()
                        .orderByDesc(SystemOperationLogEntity::getId)
                        .last("limit " + safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 100;
        }
        return Math.min(limit, 200);
    }

    private OperationLogResponse toResponse(SystemOperationLogEntity log) {
        return new OperationLogResponse(
                log.getId(),
                log.getUserId(),
                log.getMethod(),
                log.getPath(),
                log.getModule(),
                log.getOperation(),
                log.getPermissionCode(),
                Boolean.TRUE.equals(log.getSuccess()),
                log.getResponseCode(),
                log.getErrorMessage(),
                log.getTraceId(),
                log.getIp(),
                log.getUserAgent(),
                log.getDurationMs(),
                log.getCreatedAt()
        );
    }

    public record OperationAuditRecord(
            Long userId,
            String method,
            String path,
            String module,
            String operation,
            String permissionCode,
            boolean success,
            Integer responseCode,
            String errorMessage,
            String traceId,
            String ip,
            String userAgent,
            Long durationMs
    ) {
    }
}
