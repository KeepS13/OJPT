package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for admin-only draft problem creation.
 */
@Data
public class ProblemCreateDTO {

    @NotBlank(message = "题目标题不能为空")
    private String title;

    /**
     * 难度：EASY / MEDIUM / HARD
     */
    @NotBlank(message = "难度不能为空")
    private String difficulty;

    /**
     * 题面 Markdown 内容。
     */
    @NotBlank(message = "题面内容不能为空")
    private String statementMd;

    /**
     * 时间限制，单位毫秒。
     */
    @NotNull(message = "时间限制不能为空")
    private Integer timeLimitMs;

    /**
     * 内存限制，单位 KB。
     */
    @NotNull(message = "内存限制不能为空")
    private Integer memoryLimitKb;
}
