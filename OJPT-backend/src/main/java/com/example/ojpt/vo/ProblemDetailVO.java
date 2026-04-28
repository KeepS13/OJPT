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

    /**
     * 题目展示编号
     */
    private Integer problemNo;

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

    private List<ProblemTestCaseVO> sampleTestCases;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

