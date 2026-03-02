package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端/列表使用的题目简要视图。
 */
@Data
public class ProblemSimpleVO {

    private Long id;

    /**
     * 题目展示编号
     */
    private Integer problemNo;

    private String title;

    private String difficulty;

    private String status;

    private Long submitCount;

    private Long acceptedCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

