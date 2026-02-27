package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建题目草稿 DTO（任意登录用户可用）。
 */
@Data
public class ProblemCreateDTO {

    @NotBlank(message = "题目标题不能为空")
    private String title;

    /**
     * 难度：EASY/MEDIUM/HARD
     */
    @NotBlank(message = "难度不能为空")
    private String difficulty;

    /**
     * 题面 Markdown 内容。
     */
    @NotBlank(message = "题面内容不能为空")
    private String statementMd;

    /**
     * 时间限制（毫秒），可选，未填时由后端给默认值。
     */
    @NotNull(message = "时间限制不能为空")
    private Integer timeLimitMs;

    /**
     * 内存限制（KB），可选，未填时由后端给默认值。
     */
    @NotNull(message = "内存限制不能为空")
    private Integer memoryLimitKb;
}

