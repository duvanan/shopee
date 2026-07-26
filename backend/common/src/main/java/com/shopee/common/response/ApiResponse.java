package com.shopee.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<FieldErrorDetail> errors,
        ApiMeta meta
) {

    private static final String TRACE_ID = "traceId";

    public static <T> ApiResponse<T> success(T data) {
        return success("SUCCESS", "Thành công", data);
    }

    public static <T> ApiResponse<T> success(
            String code,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                true,
                code,
                message,
                data,
                null,
                ApiMeta.success(getTraceId())
        );
    }

    public static ApiResponse<Void> success(
            String code,
            String message
    ) {
        return new ApiResponse<>(
                true,
                code,
                message,
                null,
                null,
                ApiMeta.success(getTraceId())
        );
    }

    public static ApiResponse<Void> error(
            String code,
            String message,
            String path
    ) {
        return error(code, message, null, path);
    }

    public static ApiResponse<Void> error(
            String code,
            String message,
            List<FieldErrorDetail> errors,
            String path
    ) {
        return new ApiResponse<>(
                false,
                code,
                message,
                null,
                errors,
                ApiMeta.error(getTraceId(), path)
        );
    }

    private static String getTraceId() {
        return MDC.get(TRACE_ID);
    }
}
