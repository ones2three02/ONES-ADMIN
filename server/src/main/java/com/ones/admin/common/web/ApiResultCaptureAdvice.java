package com.ones.admin.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class ApiResultCaptureAdvice implements ResponseBodyAdvice<Object> {

    public static final String API_RESULT_CODE_ATTRIBUTE = "ones.api.resultCode";
    public static final String API_RESULT_MESSAGE_ATTRIBUTE = "ones.api.resultMessage";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body instanceof ApiResult<?> apiResult && request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            httpServletRequest.setAttribute(API_RESULT_CODE_ATTRIBUTE, apiResult.code());
            httpServletRequest.setAttribute(API_RESULT_MESSAGE_ATTRIBUTE, apiResult.message());
        }
        return body;
    }
}
