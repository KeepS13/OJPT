package com.example.ojpt.dto;

import lombok.Data;

/**
 * 管理员更新题目 DTO。
 */
@Data
public class ProblemUpdateDTO {

    private String title;

    /**
     * 难度：EASY/MEDIUM/HARD
     */
    private String difficulty;

    /**
     * 题面 Markdown 内容。
     */
    private String statementMd;

    /**
     * 时间限制（毫秒）。
     */
    private Integer timeLimitMs;

    /**
     * 内存限制（KB）。
     */
    private Integer memoryLimitKb;
}

