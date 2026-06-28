package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.audit.LoginAuditService;
import com.ones.admin.system.audit.OperationAuditService;
import com.ones.admin.system.dto.LoginLogQuery;
import com.ones.admin.system.dto.LoginLogResponse;
import com.ones.admin.system.dto.OperationLogQuery;
import com.ones.admin.system.dto.OperationLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/audit")
@Tag(name = "系统管理-审计日志")
@Validated
public class AuditLogController {

    private final LoginAuditService loginAuditService;
    private final OperationAuditService operationAuditService;

    public AuditLogController(
            LoginAuditService loginAuditService,
            OperationAuditService operationAuditService
    ) {
        this.loginAuditService = loginAuditService;
        this.operationAuditService = operationAuditService;
    }

    @GetMapping("/login-logs")
    @SaCheckPermission("system:audit:login-log")
    @Operation(summary = "查询登录日志")
    public ApiResult<PageResult<LoginLogResponse>> listLoginLogs(@Valid LoginLogQuery query) {
        return ApiResult.ok(loginAuditService.queryPage(query));
    }

    @GetMapping("/operation-logs")
    @SaCheckPermission("system:audit:operation-log")
    @Operation(summary = "查询操作日志")
    public ApiResult<PageResult<OperationLogResponse>> listOperationLogs(@Valid OperationLogQuery query) {
        return ApiResult.ok(operationAuditService.queryPage(query));
    }
}
