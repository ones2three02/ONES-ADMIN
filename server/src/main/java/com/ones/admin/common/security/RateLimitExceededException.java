package com.ones.admin.common.security;

import com.ones.admin.common.code.ErrorCode;
import com.ones.admin.common.exception.BusinessException;

public class RateLimitExceededException extends BusinessException {

    public RateLimitExceededException(ErrorCode errorCode) {
        super(errorCode);
    }
}
