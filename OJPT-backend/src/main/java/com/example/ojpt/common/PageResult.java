package com.example.ojpt.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应包装类
 * 
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    
    /**
     * 当前页数据列表
     */
    private List<T> records;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 当前页码（从1开始）
     */
    private long current;
    
    /**
     * 每页大小
     */
    private long size;
    
    /**
     * 总页数
     */
    private long pages;
    
    /**
     * 从 MyBatis-Plus Page 对象创建 PageResult
     */
    public static <T> PageResult<T> from(Page<T> page) {
        return PageResult.<T>builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .pages(page.getPages())
                .build();
    }
    
    /**
     * 从 MyBatis-Plus Page 对象创建 PageResult，并转换数据类型
     * 
     * @param page 原始分页对象
     * @param converter 数据转换函数
     * @param <S> 源数据类型
     * @param <T> 目标数据类型
     */
    public static <S, T> PageResult<T> from(Page<S> page, Function<S, T> converter) {
        List<T> convertedRecords = page.getRecords().stream()
                .map(converter)
                .collect(Collectors.toList());
        
        return PageResult.<T>builder()
                .records(convertedRecords)
                .total(page.getTotal())
                .current(page.getCurrent())
                .size(page.getSize())
                .pages(page.getPages())
                .build();
    }
    
    /**
     * 从已有的 List 创建 PageResult（用于内存分页场景）
     */
    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        long pages = (total + size - 1) / size;
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .current(current)
                .size(size)
                .pages(pages)
                .build();
    }
    
    /**
     * 创建空分页结果
     */
    public static <T> PageResult<T> empty(long current, long size) {
        return PageResult.<T>builder()
                .records(List.of())
                .total(0)
                .current(current)
                .size(size)
                .pages(0)
                .build();
    }
    
    /**
     * 判断是否有下一页
     */
    public boolean hasNext() {
        return current < pages;
    }
    
    /**
     * 判断是否有上一页
     */
    public boolean hasPrevious() {
        return current > 1;
    }
    
    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }
}
