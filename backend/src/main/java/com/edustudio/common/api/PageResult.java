package com.edustudio.common.api;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class PageResult<T> {

    private final List<T> records;
    private final long total;
    private final long pageNum;
    private final long pageSize;
    private final long pages;

    private PageResult(List<T> records, long total, long pageNum, long pageSize) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        return new PageResult<>(records, total, pageNum, pageSize);
    }
}
