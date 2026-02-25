package com.example.ojpt.common;

/**
 * 分页参数统一处理：默认值与上限
 */
public final class PaginationUtils {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private PaginationUtils() {}

    /**
     * 规范化页码（从 1 开始）
     */
    public static int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /**
     * 规范化每页条数（默认 10，上限 100）
     */
    public static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
