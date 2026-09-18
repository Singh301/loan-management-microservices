package com.loanmanagement.common.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@Data
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> content = List.of();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    @Builder
    public PageResponse(
            @Singular("item") List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last) {
        setContent(content);
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    public void setContent(List<T> content) {
        this.content = content == null ? List.of() : List.copyOf(content);
    }
}
