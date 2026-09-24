package com.ashraf.shared.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable JSON envelope for every paginated endpoint, so clients never depend on Spring's
 * internal Page serialization. {@code items} is passed separately from the source page
 * because callers usually map entities to DTOs with batch-loaded extras.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> PageResponse<T> of(Page<?> source, List<T> items) {
        return new PageResponse<>(
                items,
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.hasNext());
    }
}
