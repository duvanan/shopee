package com.shopee.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Toan bo ma loi cua he thong duoc quan ly tap trung o day (dung chung cho
 * moi service qua module `common`) - khong viet chuoi ma loi rai rac trong
 * code. Xem docs/architecture/api-response-standard.md.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Nhom chung, dung cho loi ha tang/khong gan voi 1 nghiep vu cu the.
    VALIDATION_ERROR(
            "VALIDATION_ERROR",
            "Dữ liệu đầu vào không hợp lệ",
            HttpStatus.BAD_REQUEST
    ),
    INVALID_REQUEST(
            "INVALID_REQUEST",
            "Yêu cầu không hợp lệ",
            HttpStatus.BAD_REQUEST
    ),
    UNAUTHORIZED(
            "UNAUTHORIZED",
            "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn",
            HttpStatus.UNAUTHORIZED
    ),
    ACCESS_DENIED(
            "ACCESS_DENIED",
            "Bạn không có quyền thực hiện chức năng này",
            HttpStatus.FORBIDDEN
    ),
    RESOURCE_NOT_FOUND(
            "RESOURCE_NOT_FOUND",
            "Không tìm thấy dữ liệu",
            HttpStatus.NOT_FOUND
    ),
    DATA_ALREADY_EXISTS(
            "DATA_ALREADY_EXISTS",
            "Dữ liệu đã tồn tại",
            HttpStatus.CONFLICT
    ),
    BUSINESS_ERROR(
            "BUSINESS_ERROR",
            "Không thể thực hiện yêu cầu",
            HttpStatus.UNPROCESSABLE_ENTITY
    ),
    INTERNAL_SERVER_ERROR(
            "INTERNAL_SERVER_ERROR",
            "Hệ thống đang xảy ra lỗi, vui lòng thử lại sau",
            HttpStatus.INTERNAL_SERVER_ERROR
    ),

    // user-service
    EMAIL_ALREADY_EXISTS(
            "EMAIL_ALREADY_EXISTS",
            "Email đã được sử dụng",
            HttpStatus.CONFLICT
    ),
    INVALID_CREDENTIALS(
            "INVALID_CREDENTIALS",
            "Email hoặc mật khẩu không đúng",
            HttpStatus.UNAUTHORIZED
    ),
    USER_NOT_FOUND(
            "USER_NOT_FOUND",
            "Không tìm thấy người dùng",
            HttpStatus.NOT_FOUND
    ),
    REFRESH_TOKEN_MISSING(
            "REFRESH_TOKEN_MISSING",
            "Thiếu refresh token",
            HttpStatus.UNAUTHORIZED
    ),
    REFRESH_TOKEN_INVALID(
            "REFRESH_TOKEN_INVALID",
            "Refresh token không hợp lệ hoặc đã hết hạn",
            HttpStatus.UNAUTHORIZED
    ),

    // product-service
    PRODUCT_NOT_FOUND(
            "PRODUCT_NOT_FOUND",
            "Không tìm thấy sản phẩm",
            HttpStatus.NOT_FOUND
    ),
    CATEGORY_NOT_FOUND(
            "CATEGORY_NOT_FOUND",
            "Danh mục không tồn tại",
            HttpStatus.BAD_REQUEST
    ),
    CATEGORY_HAS_PRODUCTS(
            "CATEGORY_HAS_PRODUCTS",
            "Không thể xoá danh mục đang có sản phẩm",
            HttpStatus.CONFLICT
    );

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
