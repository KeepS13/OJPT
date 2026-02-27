package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.PaginationUtils;
import com.example.ojpt.dto.SubmissionCreateDTO;
import com.example.ojpt.dto.SubmissionStatusUpdateDTO;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.Submission;
import com.example.ojpt.entity.UserProblemProgress;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.SubmissionMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.SubmissionService;
import com.example.ojpt.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_AC = "AC";

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final UserProblemProgressMapper userProblemProgressMapper;

    @Override
    @Transactional
    public SubmissionVO createSubmission(Long userId, SubmissionCreateDTO dto) {
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }
        Problem problem = problemMapper.selectById(dto.getProblemId());
        if (problem == null || Objects.equals(problem.getIsDeleted(), 1)) {
            throw BusinessException.notFound("题目不存在");
        }

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(dto.getProblemId());
        submission.setLanguage(dto.getLanguage());
        submission.setSourceCode(dto.getSourceCode());
        submission.setStatus(STATUS_QUEUED);

        submissionMapper.insert(submission);

        // 更新题目统计：提交次数 +1
        Long submitCount = problem.getSubmitCount() == null ? 0L : problem.getSubmitCount();
        problem.setSubmitCount(submitCount + 1);
        problemMapper.updateById(problem);

        // 更新/创建用户进度：至少为 ATTEMPTED
        upsertUserProgressOnSubmit(userId, dto.getProblemId(), submission.getId(), false);

        return toVO(submission);
    }

    @Override
    public SubmissionVO getSubmission(Long requesterUserId, Long submissionId, boolean isAdmin) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw BusinessException.notFound("提交记录不存在");
        }
        if (!isAdmin && (requesterUserId == null || !requesterUserId.equals(submission.getUserId()))) {
            throw BusinessException.forbidden("无权查看该提交");
        }
        return toVO(submission);
    }

    @Override
    public PageResult<SubmissionVO> listMySubmissions(Long userId, Long problemId, Integer page, Integer size) {
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);

        Page<Submission> pageParam = new Page<>(p, s);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .orderByDesc(Submission::getCreatedAt);
        if (problemId != null) {
            wrapper.eq(Submission::getProblemId, problemId);
        }

        Page<Submission> result = submissionMapper.selectPage(pageParam, wrapper);
        return PageResult.from(result, this::toVO);
    }

    @Override
    @Transactional
    public void updateSubmissionStatus(Long submissionId, SubmissionStatusUpdateDTO dto) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw BusinessException.notFound("提交记录不存在");
        }

        String oldStatus = submission.getStatus();
        String newStatus = dto.getStatus();

        submission.setStatus(newStatus);
        if (dto.getTimeMs() != null) {
            submission.setTimeMs(dto.getTimeMs());
        }
        if (dto.getMemoryKb() != null) {
            submission.setMemoryKb(dto.getMemoryKb());
        }
        if (dto.getCompileMessage() != null) {
            submission.setCompileMessage(dto.getCompileMessage());
        }
        if (dto.getJudgeMessage() != null) {
            submission.setJudgeMessage(dto.getJudgeMessage());
        }

        submissionMapper.updateById(submission);

        Problem problem = problemMapper.selectById(submission.getProblemId());
        if (problem != null && !Objects.equals(problem.getIsDeleted(), 1)) {
            Long accepted = problem.getAcceptedCount() == null ? 0L : problem.getAcceptedCount();
            if (!STATUS_AC.equals(oldStatus) && STATUS_AC.equals(newStatus)) {
                problem.setAcceptedCount(accepted + 1);
            } else if (STATUS_AC.equals(oldStatus) && !STATUS_AC.equals(newStatus) && accepted > 0) {
                problem.setAcceptedCount(accepted - 1);
            }
            problemMapper.updateById(problem);
        }

        // 更新用户进度：如果变为 AC，则标记为 SOLVED
        boolean solved = STATUS_AC.equals(newStatus);
        upsertUserProgressOnSubmit(submission.getUserId(), submission.getProblemId(), submission.getId(), solved);
    }

    private void upsertUserProgressOnSubmit(Long userId, Long problemId, Long submissionId, boolean solved) {
        UserProblemProgress progress = userProblemProgressMapper.selectOne(
                new LambdaQueryWrapper<UserProblemProgress>()
                        .eq(UserProblemProgress::getUserId, userId)
                        .eq(UserProblemProgress::getProblemId, problemId)
        );

        if (progress == null) {
            progress = new UserProblemProgress();
            progress.setUserId(userId);
            progress.setProblemId(problemId);
            progress.setLastSubmissionId(submissionId);
            progress.setStatus(solved ? "SOLVED" : "ATTEMPTED");
            userProblemProgressMapper.insert(progress);
        } else {
            progress.setLastSubmissionId(submissionId);
            if (solved) {
                progress.setStatus("SOLVED");
            } else if ("UNSOLVED".equals(progress.getStatus())) {
                progress.setStatus("ATTEMPTED");
            }
            userProblemProgressMapper.updateById(progress);
        }
    }

    private SubmissionVO toVO(Submission submission) {
        SubmissionVO vo = new SubmissionVO();
        vo.setId(submission.getId());
        vo.setProblemId(submission.getProblemId());
        vo.setLanguage(submission.getLanguage());
        vo.setStatus(submission.getStatus());
        vo.setTimeMs(submission.getTimeMs());
        vo.setMemoryKb(submission.getMemoryKb());
        vo.setCreatedAt(submission.getCreatedAt());
        return vo;
    }
}

