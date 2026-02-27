package com.example.ojpt.vo;

import lombok.Data;

import java.util.List;

/**
 * 学员端题库列表项视图。
 */
@Data
public class ProblemListItemVO {

    private Long id;

    private String title;

    private String difficulty;

    /**
     * 当前用户的做题状态（UNSOLVED/ATTEMPTED/SOLVED），未登录或无记录时可为 null。
     */
    private String status;

    private Long submitCount;

    private Long acceptedCount;

    /**
     * 通过率（0-100），可空。
     */
    private Double acceptanceRate;

    /**
     * 标签列表（可选）。
     */
    private List<TagVO> tags;
}

