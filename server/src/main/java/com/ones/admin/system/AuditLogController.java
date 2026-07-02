package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.audit.AuditRetentionService;
import com.ones.admin.system.audit.LoginAuditService;
import com.ones.admin.system.audit.OperationAuditService;
import com.ones.admin.system.dto.AuditRetentionCleanupResponse;
import com.ones.admin.system.dto.AuditRetentionSummaryResponse;
import com.ones.admin.system.dto.LoginLogQuery;
import com.ones.admin.system.dto.LoginLogResponse;
import com.ones.admin.system.dto.OperationLogQuery;
import com.ones.admin.system.dto.OperationLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/system/audit")
@Tag(name = "系统管理-审计日志")
@Validated
@ApiResourceMetadata(
        owner = "审计与安全组",
        audience = "SECURITY_AUDITOR",
        sinceVersion = "v0.0.4",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class AuditLogController {

    private final AuditRetentionService auditRetentionService;
    private final LoginAuditService loginAuditService;
    private final OperationAuditService operationAuditService;

    public AuditLogController(
            AuditRetentionService auditRetentionService,
            LoginAuditService loginAuditService,
            OperationAuditService operationAuditService
    ) {
        this.auditRetentionService = auditRetentionService;
        this.loginAuditService = loginAuditService;
        this.operationAuditService = operationAuditService;
    }

    @GetMapping("/retention")
    @SaCheckPermission("system:audit:retention")
    @Operation(summary = "查询审计日志保留策略")
    @ApiResourceMetadata(sinceVersion = "v0.0.47")
    public ApiResult<AuditRetentionSummaryResponse> getAuditRetention() {
        return ApiResult.ok(auditRetentionService.summarize());
    }

    @PostMapping("/retention/cleanup")
    @SaCheckPermission("system:audit:retention")
    @RepeatSubmit(intervalMillis = 30_000, message = "审计日志清理正在执行，请勿重复提交")
    @Operation(summary = "清理过期审计日志")
    @ApiResourceMetadata(sinceVersion = "v0.0.47", riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<AuditRetentionCleanupResponse> cleanupExpiredAuditLogs() {
        return ApiResult.ok(auditRetentionService.cleanupExpiredLogs());
    }

    @GetMapping("/login-logs")
    @SaCheckPermission("system:audit:login-log")
    @Operation(summary = "查询登录日志")
    public ApiResult<PageResult<LoginLogResponse>> listLoginLogs(@Valid LoginLogQuery query) {
        return ApiResult.ok(loginAuditService.queryPage(query));
    }

    @GetMapping("/login-logs/export")
    @SaCheckPermission("system:audit:login-log")
    @Operation(summary = "导出登录日志")
    public ResponseEntity<String> exportLoginLogs(@Valid LoginLogQuery query) {
        return csvResponse("ones-login-logs.csv", loginAuditService.exportCsv(query));
    }

    @GetMapping("/operation-logs")
    @SaCheckPermission("system:audit:operation-log")
    @Operation(summary = "查询操作日志")
    public ApiResult<PageResult<OperationLogResponse>> listOperationLogs(@Valid OperationLogQuery query) {
        return ApiResult.ok(operationAuditService.queryPage(query));
    }

    @GetMapping("/operation-logs/export")
    @SaCheckPermission("system:audit:operation-log")
    @Operation(summary = "导出操作日志")
    public ResponseEntity<String> exportOperationLogs(@Valid OperationLogQuery query) {
        return csvResponse("ones-operation-logs.csv", operationAuditService.exportCsv(query));
    }

    private ResponseEntity<String> csvResponse(String filename, String body) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
