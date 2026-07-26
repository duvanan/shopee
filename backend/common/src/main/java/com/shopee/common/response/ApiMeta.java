package com.shopee.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMeta(
        Instant timestamp,
        String traceId,
        String path
) {

    public static ApiMeta success(String traceId) {
        return new ApiMeta(
                Instant.now(),
                traceId,
                null
        );
    }

    public static ApiMeta error(String traceId, String path) {
        return new ApiMeta(
                Instant.now(),
                traceId,
                path
        );
    }
}
