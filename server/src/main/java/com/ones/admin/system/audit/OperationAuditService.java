package com.ones.admin.system.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.OperationLogQuery;
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

    public PageResult<OperationLogResponse> queryPage(OperationLogQuery query) {
        IPage<SystemOperationLogEntity> page = operationLogMapper.selectPage(
                query.toMyBatisPage(),
                buildQueryWrapper(query)
        );
        List<OperationLogResponse> records = page.getRecords()
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(page, records);
    }

    private LambdaQueryWrapper<SystemOperationLogEntity> buildQueryWrapper(OperationLogQuery query) {
        LambdaQueryWrapper<SystemOperationLogEntity> wrapper = new LambdaQueryWrapper<SystemOperationLogEntity>()
                .orderByDesc(SystemOperationLogEntity::getId);
        if (query.getUserId() != null) {
            wrapper.eq(SystemOperationLogEntity::getUserId, query.getUserId());
        }
        if (hasText(query.getMethod())) {
            wrapper.eq(SystemOperationLogEntity::getMethod, query.getMethod().trim().toUpperCase());
        }
        if (hasText(query.getPath())) {
            wrapper.like(SystemOperationLogEntity::getPath, query.getPath().trim());
        }
        if (query.getSuccess() != null) {
            wrapper.eq(SystemOperationLogEntity::getSuccess, query.getSuccess());
        }
        if (query.getResponseCode() != null) {
            wrapper.eq(SystemOperationLogEntity::getResponseCode, query.getResponseCode());
        }
        if (hasText(query.getTraceId())) {
            wrapper.eq(SystemOperationLogEntity::getTraceId, query.getTraceId().trim());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(SystemOperationLogEntity::getCreatedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(SystemOperationLogEntity::getCreatedAt, query.getEndTime());
        }
        return wrapper;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
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
