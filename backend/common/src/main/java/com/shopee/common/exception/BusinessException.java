package com.shopee.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customMessage;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public BusinessException(
            ErrorCode errorCode,
            String customMessage
    ) {
        super(customMessage);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }

    public String getResponseMessage() {
        return customMessage != null && !customMessage.isBlank()
                ? customMessage
                : errorCode.getMessage();
    }
}
