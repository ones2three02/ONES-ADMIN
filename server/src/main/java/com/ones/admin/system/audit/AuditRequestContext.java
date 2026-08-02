package com.ones.admin.system.audit;

import jakarta.servlet.http.HttpServletRequest;

public record AuditRequestContext(
        String ip,
        String userAgent,
        String traceId
) {

    public static final String TRACE_ID_ATTRIBUTE = "ones.api.traceId";

    public static AuditRequestContext from(HttpServletRequest request) {
        return new AuditRequestContext(
                clientIp(request),
                truncate(request.getHeader("User-Agent"), 512),
                traceId(request)
        );
    }

    public static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return truncate(forwardedFor.split(",")[0].trim(), 64);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return truncate(realIp.trim(), 64);
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    public static String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TRACE_ID_ATTRIBUTE);
        if (traceId != null) {
            return truncate(String.valueOf(traceId), 64);
        }
        String headerTraceId = request.getHeader("X-Trace-Id");
        return truncate(headerTraceId, 64);
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
