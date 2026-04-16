package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端题目列表项视图（草稿池/已发布/已归档）。
 */
@Data
public class AdminProblemListItemVO {

    private Long id;

    private Integer problemNo;

    private String title;

    private String difficulty;

    /**
     * 题目状态：DRAFT/PUBLISHED/ARCHIVED
     */
    private String status;

    private Long submitCount;

    private Long acceptedCount;

    /**
     * 通过率（0-100），可空。
     */
    private Double acceptanceRate;

    private List<TagVO> tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

