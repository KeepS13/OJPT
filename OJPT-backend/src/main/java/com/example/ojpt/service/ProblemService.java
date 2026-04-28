package com.example.ojpt.service;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.dto.ProblemCreateDTO;
import com.example.ojpt.dto.ProblemTestCaseBatchUpdateDTO;
import com.example.ojpt.dto.ProblemUpdateDTO;
import com.example.ojpt.vo.AdminProblemListItemVO;
import com.example.ojpt.vo.ProblemDetailVO;
import com.example.ojpt.vo.ProblemListItemVO;
import com.example.ojpt.vo.ProblemSimpleVO;
import com.example.ojpt.vo.ProblemTestCaseVO;

import java.util.List;

public interface ProblemService {

    ProblemSimpleVO createDraft(Long userId, ProblemCreateDTO dto);

    void updateProblem(Long problemId, ProblemUpdateDTO dto);

    void publishProblem(Long problemId, Long adminUserId);

    void archiveProblem(Long problemId, Long adminUserId);

    ProblemSimpleVO getProblem(Long problemId);

    PageResult<AdminProblemListItemVO> queryAdminProblems(
            Integer page,
            Integer size,
            String keyword,
            String difficulty,
            Long tagId,
            String status,
            String orderBy
    );

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

    ProblemDetailVO getProblemDetail(Long problemId, Long userId);

    ProblemDetailVO getProblemDetailByNo(Integer problemNo, Long userId);

    List<ProblemTestCaseVO> getProblemTestCases(Long problemId);

    void replaceProblemTestCases(Long problemId, ProblemTestCaseBatchUpdateDTO dto);
}
