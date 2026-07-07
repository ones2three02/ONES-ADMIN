package com.ones.admin.system.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ones.admin.common.event.SystemEventPayload;
import com.ones.admin.common.event.SystemEventPublisher;
import com.ones.admin.common.web.CsvExportUtils;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.OperationLogQuery;
import com.ones.admin.system.dto.OperationLogResponse;
import com.ones.admin.system.entity.SystemOperationLogEntity;
import com.ones.admin.system.mapper.SystemOperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationAuditService {

    private static final String CSV_HEADER = "id,userId,method,path,module,operation,permissionCode,success,"
            + "responseCode,errorMessage,traceId,ip,userAgent,durationMs,createdAt";

    private final SystemOperationLogMapper operationLogMapper;
    private final SystemEventPublisher eventPublisher;

    public OperationAuditService(SystemOperationLogMapper operationLogMapper, SystemEventPublisher eventPublisher) {
        this.operationLogMapper = operationLogMapper;
        this.eventPublisher = eventPublisher;
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
        eventPublisher.publish(SystemEventPayload.of(
                "audit.operation.recorded",
                "ones.audit.operation.recorded",
                record.traceId(),
                operationAuditPayload(log)
        ));
    }

    private Map<String, Object> operationAuditPayload(SystemOperationLogEntity log) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", log.getUserId());
        payload.put("method", log.getMethod());
        payload.put("path", log.getPath());
        payload.put("module", log.getModule());
        payload.put("operation", log.getOperation());
        payload.put("permissionCode", log.getPermissionCode());
        payload.put("success", log.getSuccess());
        payload.put("responseCode", log.getResponseCode());
        payload.put("errorMessage", log.getErrorMessage());
        payload.put("ip", log.getIp());
        payload.put("userAgent", log.getUserAgent());
        payload.put("durationMs", log.getDurationMs());
        return payload;
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

    public String exportCsv(OperationLogQuery query) {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        operationLogMapper.selectList(buildQueryWrapper(query))
                .forEach(log -> csv.append(CsvExportUtils.row(
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
                )).append('\n'));
        return csv.toString();
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
        if (hasText(query.getModule())) {
            wrapper.like(SystemOperationLogEntity::getModule, query.getModule().trim());
        }
        if (hasText(query.getOperation())) {
            wrapper.like(SystemOperationLogEntity::getOperation, query.getOperation().trim());
        }
        if (hasText(query.getPermissionCode())) {
            wrapper.like(SystemOperationLogEntity::getPermissionCode, query.getPermissionCode().trim());
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
