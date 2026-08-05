package com.shopee.user.entity;

/** Chi liet ke hanh dong da co noi goi thuc te - mo rong dan khi them tinh nang moi. */
public enum AuditAction {
    CREATE_USER,
    UPDATE_USER,
    ASSIGN_ROLE,
    REMOVE_ROLE,
    LOCK_USER,
    UNLOCK_USER,
    CREATE_ROLE,
    UPDATE_ROLE,
    DELETE_ROLE,
    UPDATE_ROLE_PERMISSIONS
}
