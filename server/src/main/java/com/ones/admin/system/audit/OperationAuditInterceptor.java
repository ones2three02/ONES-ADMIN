package com.ones.admin.system.audit;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.web.ApiResultCaptureAdvice;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

@Component
public class OperationAuditInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "ones.operation.startTime";

    private final OperationAuditService operationAuditService;

    public OperationAuditInterceptor(OperationAuditService operationAuditService) {
        this.operationAuditService = operationAuditService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldRecord(request, handler)) {
            request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        if (!shouldRecord(request, handler)) {
            return;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        AuditRequestContext context = AuditRequestContext.from(request);
        Integer apiCode = apiResultCode(request);
        boolean success = exception == null
                && response.getStatus() < HttpServletResponse.SC_BAD_REQUEST
                && (apiCode == null || Objects.equals(apiCode, CommonErrorCode.SUCCESS.code()));
        operationAuditService.record(new OperationAuditService.OperationAuditRecord(
                currentUserId(),
                request.getMethod(),
                request.getRequestURI(),
                moduleName(handlerMethod),
                operationName(handlerMethod),
                permissionCode(handlerMethod),
                success,
                apiCode,
                success ? null : errorMessage(request, exception),
                context.traceId(),
                context.ip(),
                context.userAgent(),
                durationMs(request)
        ));
    }

    private boolean shouldRecord(HttpServletRequest request, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return false;
        }
        String method = request.getMethod();
        boolean writeMethod = HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method);
        return writeMethod && request.getRequestURI().startsWith("/api/system/");
    }

    private Long currentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    private Integer apiResultCode(HttpServletRequest request) {
        Object code = request.getAttribute(ApiResultCaptureAdvice.API_RESULT_CODE_ATTRIBUTE);
        if (code instanceof Integer integerCode) {
            return integerCode;
        }
        return null;
    }

    private String errorMessage(HttpServletRequest request, Exception exception) {
        Object message = request.getAttribute(ApiResultCaptureAdvice.API_RESULT_MESSAGE_ATTRIBUTE);
        if (message != null) {
            return String.valueOf(message);
        }
        return exception == null ? null : exception.getMessage();
    }

    private Long durationMs(HttpServletRequest request) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime instanceof Long startedAt) {
            return System.currentTimeMillis() - startedAt;
        }
        return null;
    }

    private String moduleName(HandlerMethod handlerMethod) {
        Tag tag = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Tag.class);
        return tag == null ? null : tag.name();
    }

    private String operationName(HandlerMethod handlerMethod) {
        Operation operation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Operation.class);
        return operation == null ? null : operation.summary();
    }

    private String permissionCode(HandlerMethod handlerMethod) {
        SaCheckPermission permission = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), SaCheckPermission.class);
        if (permission == null) {
            permission = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), SaCheckPermission.class);
        }
        if (permission == null || permission.value().length == 0) {
            return null;
        }
        return String.join(",", permission.value());
    }
}
