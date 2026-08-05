package com.shopee.user.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Metadata thiet bi/request luu kem session tren Redis - chi phuc vu hien
 * thi "ban dang dang nhap o dau" (GET /api/users/auth/sessions), khong dung
 * de xac thuc/gate quyen gi. ipAddress lay tu request.getRemoteAddr(): phia
 * sau api-gateway se ra dia chi cua Gateway chu khong phai client that,
 * chap nhan duoc vi day chi la thong tin hien thi, khong phai bao mat.
 */
public record DeviceInfo(String ipAddress, String userAgent) {

    public static DeviceInfo from(HttpServletRequest request) {
        return new DeviceInfo(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
