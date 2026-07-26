package com.shopee.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageData<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageData<T> from(Page<T> pageData) {
        return new PageData<>(
                pageData.getContent(),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                pageData.isFirst(),
                pageData.isLast()
        );
    }

    public static <E, T> PageData<T> from(
            Page<E> pageData,
            Function<E, T> mapper
    ) {
        List<T> items = pageData.getContent()
                .stream()
                .map(mapper)
                .toList();

        return new PageData<>(
                items,
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                pageData.isFirst(),
                pageData.isLast()
        );
    }
}
