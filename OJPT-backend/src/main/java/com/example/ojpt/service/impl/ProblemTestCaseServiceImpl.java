package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ojpt.dto.ProblemTestCaseBatchUpdateDTO;
import com.example.ojpt.dto.ProblemTestCaseItemDTO;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.ProblemTestCase;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.ProblemTestCaseMapper;
import com.example.ojpt.service.ProblemTestCaseService;
import com.example.ojpt.vo.ProblemTestCaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemTestCaseServiceImpl implements ProblemTestCaseService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String CASE_TYPE_SAMPLE = "SAMPLE";
    private static final String CASE_TYPE_HIDDEN = "HIDDEN";

    private final ProblemMapper problemMapper;
    private final ProblemTestCaseMapper problemTestCaseMapper;

    @Override
    public List<ProblemTestCaseVO> getSampleTestCasesByProblemNo(Integer problemNo) {
        if (problemNo == null || problemNo <= 0) {
            throw BusinessException.badRequest("题号不合法");
        }

        Problem problem = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getProblemNo, problemNo)
                        .eq(Problem::getIsDeleted, 0)
                        .eq(Problem::getStatus, STATUS_PUBLISHED)
        );
        if (problem == null) {
            throw BusinessException.notFound("题目");
        }

        return loadProblemTestCaseVos(problem.getId(), CASE_TYPE_SAMPLE);
    }

    @Override
    public List<ProblemTestCaseVO> getProblemTestCases(Long problemId) {
        Problem problem = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId)
                        .eq(Problem::getIsDeleted, 0)
        );
        if (problem == null) {
            throw BusinessException.notFound("题目");
        }

        return problemTestCaseMapper.selectList(
                new LambdaQueryWrapper<ProblemTestCase>()
                        .eq(ProblemTestCase::getProblemId, problemId)
                        .orderByAsc(ProblemTestCase::getCaseType)
                        .orderByAsc(ProblemTestCase::getSortOrder)
                        .orderByAsc(ProblemTestCase::getId)
        ).stream().map(this::toProblemTestCaseVO).toList();
    }

    @Override
    @Transactional
    public void replaceProblemTestCases(Long problemId, ProblemTestCaseBatchUpdateDTO dto) {
        Problem problem = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getId, problemId)
                        .eq(Problem::getIsDeleted, 0)
        );
        if (problem == null) {
            throw BusinessException.notFound("题目");
        }

        List<ProblemTestCaseItemDTO> items = dto.getCases() == null ? List.of() : dto.getCases();
        for (ProblemTestCaseItemDTO item : items) {
            String caseType = item.getCaseType() == null ? "" : item.getCaseType().trim().toUpperCase();
            if (!CASE_TYPE_SAMPLE.equals(caseType) && !CASE_TYPE_HIDDEN.equals(caseType)) {
                throw BusinessException.badRequest("测试用例类型只能是 SAMPLE 或 HIDDEN");
            }
            item.setCaseType(caseType);
        }

        problemTestCaseMapper.delete(
                new LambdaQueryWrapper<ProblemTestCase>()
                        .eq(ProblemTestCase::getProblemId, problemId)
        );

        for (ProblemTestCaseItemDTO item : items) {
            ProblemTestCase testCase = new ProblemTestCase()
                    .setProblemId(problemId)
                    .setCaseType(item.getCaseType())
                    .setSortOrder(item.getSortOrder())
                    .setInputText(item.getInputText())
                    .setExpectedOutput(item.getExpectedOutput())
                    .setExplanation(item.getExplanation());
            problemTestCaseMapper.insert(testCase);
        }
    }

    private List<ProblemTestCaseVO> loadProblemTestCaseVos(Long problemId, String caseType) {
        return problemTestCaseMapper.selectList(
                new LambdaQueryWrapper<ProblemTestCase>()
                        .eq(ProblemTestCase::getProblemId, problemId)
                        .eq(ProblemTestCase::getCaseType, caseType)
                        .orderByAsc(ProblemTestCase::getSortOrder)
                        .orderByAsc(ProblemTestCase::getId)
        ).stream().map(this::toProblemTestCaseVO).toList();
    }

    private ProblemTestCaseVO toProblemTestCaseVO(ProblemTestCase testCase) {
        ProblemTestCaseVO vo = new ProblemTestCaseVO();
        vo.setId(testCase.getId());
        vo.setCaseType(testCase.getCaseType());
        vo.setSortOrder(testCase.getSortOrder());
        vo.setInputText(testCase.getInputText());
        vo.setExpectedOutput(testCase.getExpectedOutput());
        vo.setExplanation(testCase.getExplanation());
        return vo;
    }
}
