package com.example.ojpt.service;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.dto.ProblemCreateDTO;
import com.example.ojpt.dto.ProblemUpdateDTO;
import com.example.ojpt.vo.ProblemDetailVO;
import com.example.ojpt.vo.ProblemListItemVO;
import com.example.ojpt.vo.ProblemSimpleVO;

public interface ProblemService {

    /**
     * 任意登录用户创建题目草稿。
     */
    ProblemSimpleVO createDraft(Long userId, ProblemCreateDTO dto);

    /**
     * 管理员更新题目信息。
     */
    void updateProblem(Long problemId, ProblemUpdateDTO dto);

    /**
     * 管理员发布题目到正式题库。
     */
    void publishProblem(Long problemId, Long adminUserId);

    /**
     * 管理员下架/归档题目。
     */
    void archiveProblem(Long problemId, Long adminUserId);

    /**
     * 根据 ID 获取题目（主要用于管理端查看）。
     */
    ProblemSimpleVO getProblem(Long problemId);

    /**
     * 学员端分页查询题目列表（支持匿名访问，提交状态依赖登录用户）。
     *
     * @param userId   当前用户ID，未登录时为 null
     * @param page     页码
     * @param size     每页大小
     * @param keyword  关键字（按标题模糊匹配）
     * @param difficulty 难度过滤（可空）
     * @param tagId    标签过滤（可空）
     * @param status   做题状态过滤（UNSOLVED/ATTEMPTED/SOLVED，可空）
     * @param orderBy  排序字段（如 ID/DIFFICULTY/ACCEPTANCE，可空）
     */
    PageResult<ProblemListItemVO> queryProblems(
            Long userId,
            Integer page,
            Integer size,
            String keyword,
            String difficulty,
            Long tagId,
            String status,
            String orderBy
    );

    /**
     * 学员端获取题目详情（支持匿名访问，status 依赖登录用户）。
     */
    ProblemDetailVO getProblemDetail(Long problemId, Long userId);
}

