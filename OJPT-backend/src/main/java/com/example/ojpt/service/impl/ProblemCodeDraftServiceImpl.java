package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ojpt.dto.ProblemCodeDraftSaveDTO;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.ProblemCodeDraft;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.mapper.ProblemCodeDraftMapper;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.service.ProblemCodeDraftService;
import com.example.ojpt.vo.ProblemCodeDraftVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProblemCodeDraftServiceImpl implements ProblemCodeDraftService {

    private final ProblemMapper problemMapper;
    private final ProblemCodeDraftMapper problemCodeDraftMapper;

    @Override
    @Transactional(readOnly = true)
    public ProblemCodeDraftVO getDraft(Long userId, Integer problemNo, String language) {
        if (userId == null) {
            throw BusinessException.unauthorized("未登录");
        }
        if (language == null || language.isBlank()) {
            throw BusinessException.badRequest("语言不能为空");
        }

        Problem problem = getProblemByNo(problemNo);
        ProblemCodeDraft draft = findDraft(userId, problem.getId(), language.trim());
        return draft == null ? null : toVO(problem, draft);
    }

    @Override
    @Transactional
    public ProblemCodeDraftVO saveDraft(Long userId, Integer problemNo, ProblemCodeDraftSaveDTO dto) {
        if (userId == null) {
            throw BusinessException.unauthorized("未登录");
        }

        String language = dto.getLanguage().trim();
        Problem problem = getProblemByNo(problemNo);
        ProblemCodeDraft draft = findDraft(userId, problem.getId(), language);

        if (draft == null) {
            draft = new ProblemCodeDraft()
                    .setUserId(userId)
                    .setProblemId(problem.getId())
                    .setLanguage(language)
                    .setSourceCode(dto.getSourceCode());
            problemCodeDraftMapper.insert(draft);
        } else {
            draft.setSourceCode(dto.getSourceCode());
            problemCodeDraftMapper.updateById(draft);
        }

        return toVO(problem, draft);
    }

    private Problem getProblemByNo(Integer problemNo) {
        Problem problem = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getProblemNo, problemNo)
                        .eq(Problem::getIsDeleted, 0)
        );
        if (problem == null || Objects.equals(problem.getIsDeleted(), 1)) {
            throw BusinessException.notFound("题目");
        }
        return problem;
    }

    private ProblemCodeDraft findDraft(Long userId, Long problemId, String language) {
        return problemCodeDraftMapper.selectOne(
                new LambdaQueryWrapper<ProblemCodeDraft>()
                        .eq(ProblemCodeDraft::getUserId, userId)
                        .eq(ProblemCodeDraft::getProblemId, problemId)
                        .eq(ProblemCodeDraft::getLanguage, language)
        );
    }

    private ProblemCodeDraftVO toVO(Problem problem, ProblemCodeDraft draft) {
        return new ProblemCodeDraftVO()
                .setProblemNo(problem.getProblemNo())
                .setLanguage(draft.getLanguage())
                .setSourceCode(draft.getSourceCode())
                .setUpdatedAt(draft.getUpdatedAt());
    }
}
