package com.ones.admin.common.repeatsubmit;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.exception.BusinessException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Aspect
@Component
@ConditionalOnProperty(prefix = "ones.security.repeat-submit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RepeatSubmitAspect {

    private static final String KEY_PREFIX = "ones:repeat-submit:";

    private final RepeatSubmitTicketStore ticketStore;
    private final ObjectMapper objectMapper;

    public RepeatSubmitAspect(RepeatSubmitTicketStore ticketStore, ObjectMapper objectMapper) {
        this.ticketStore = ticketStore;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatSubmit repeatSubmit) throws Throwable {
        Duration ttl = Duration.ofMillis(Math.max(repeatSubmit.intervalMillis(), 500));
        String key = buildKey(joinPoint);
        if (!ticketStore.tryLock(key, ttl)) {
            throw new BusinessException(CommonErrorCode.REPEAT_SUBMIT, repeatSubmit.message());
        }
        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String rawKey = currentLoginId()
                + "|"
                + signature.getDeclaringTypeName()
                + "."
                + signature.getName()
                + "|"
                + argumentDigest(joinPoint.getArgs());
        return KEY_PREFIX + sha256(rawKey);
    }

    private String currentLoginId() {
        if (!StpUtil.isLogin()) {
            return "anonymous";
        }
        return String.valueOf(StpUtil.getLoginId());
    }

    private String argumentDigest(Object[] args) {
        List<Object> normalizedArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof ServletRequest || arg instanceof ServletResponse) {
                continue;
            }
            if (arg instanceof MultipartFile multipartFile) {
                normalizedArgs.add(new MultipartFileDigest(
                        multipartFile.getOriginalFilename(),
                        multipartFile.getSize()
                ));
                continue;
            }
            normalizedArgs.add(arg);
        }
        try {
            return sha256(objectMapper.writeValueAsString(normalizedArgs));
        } catch (JsonProcessingException exception) {
            return sha256(String.valueOf(normalizedArgs));
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
        }
    }

    private record MultipartFileDigest(String originalFilename, long size) {
    }
}
