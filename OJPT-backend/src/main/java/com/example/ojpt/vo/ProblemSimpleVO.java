package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 题面 Markdown（编辑页需要）。
     */
    private String statementMd;

    private Integer timeLimitMs;

    private Integer memoryLimitKb;

    private List<TagVO> tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

