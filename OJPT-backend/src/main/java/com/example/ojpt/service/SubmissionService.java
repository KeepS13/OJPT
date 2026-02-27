package com.example.ojpt.service;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.dto.SubmissionCreateDTO;
import com.example.ojpt.dto.SubmissionStatusUpdateDTO;
import com.example.ojpt.vo.SubmissionVO;

public interface SubmissionService {

    /**
     * 创建提交（stub 阶段仅落库并返回 QUEUED 状态）。
     */
    SubmissionVO createSubmission(Long userId, SubmissionCreateDTO dto);

    /**
     * 查询单个提交详情（普通用户仅能查看自己的提交，管理员可查看全部）。
     */
    SubmissionVO getSubmission(Long requesterUserId, Long submissionId, boolean isAdmin);

    /**
     * 查询当前用户的提交列表（按题目过滤）。
     */
    PageResult<SubmissionVO> listMySubmissions(Long userId, Long problemId, Integer page, Integer size);

    /**
     * 管理员修改提交状态（用于模拟判题结果），并联动更新题目统计与用户进度。
     */
    void updateSubmissionStatus(Long submissionId, SubmissionStatusUpdateDTO dto);
}

