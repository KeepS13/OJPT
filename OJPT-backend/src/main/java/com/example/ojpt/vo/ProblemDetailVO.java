package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学员端题目详情视图。
 */
@Data
public class ProblemDetailVO {

    private Long id;

    private String title;

    private String difficulty;

    private String status;

    private String statementMd;

    private Integer timeLimitMs;

    private Integer memoryLimitKb;

    private Long submitCount;

    private Long acceptedCount;

    private Double acceptanceRate;

    private List<TagVO> tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

