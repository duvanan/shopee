package com.shopee.product.dto;

import com.shopee.product.entity.AttributeSet;

import java.util.UUID;

public record AttributeSetResponse(
        UUID id,
        String code,
        String name,
        String description,
        String status,
        int versionNo,
        boolean isSystem,
        int groupCount,
        int attributeCount,
        long productTypeCount
) {
    public static AttributeSetResponse from(AttributeSet set, int groupCount, int attributeCount, long productTypeCount) {
        return new AttributeSetResponse(
                set.getId(), set.getCode(), set.getName(), set.getDescription(),
                set.getStatus().name(), set.getVersionNo(), set.isSystem(),
                groupCount, attributeCount, productTypeCount
        );
    }
}
