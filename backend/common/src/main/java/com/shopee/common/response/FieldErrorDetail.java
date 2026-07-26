package com.shopee.common.response;

public record FieldErrorDetail(
        String field,
        String message
) {
}
