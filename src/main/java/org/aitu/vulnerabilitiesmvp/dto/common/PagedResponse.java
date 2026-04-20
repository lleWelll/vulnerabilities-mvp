package org.aitu.vulnerabilitiesmvp.dto.common;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public PagedResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    @Override
    public List<T> content() {
        return List.copyOf(content);
    }
}
