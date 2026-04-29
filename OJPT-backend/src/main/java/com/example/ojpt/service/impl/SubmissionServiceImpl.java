package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ojpt.common.PageResult;
import com.example.ojpt.dto.CodeRunCaseDTO;
import com.example.ojpt.dto.CodeRunDTO;
import com.example.ojpt.dto.SubmissionCreateDTO;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.ProblemTestCase;
import com.example.ojpt.entity.Submission;
import com.example.ojpt.entity.SubmissionCaseResult;
import com.example.ojpt.entity.UserProblemProgress;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.judge.CodeExecutionResult;
import com.example.ojpt.judge.CodeExecutionService;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.ProblemTestCaseMapper;
import com.example.ojpt.mapper.SubmissionCaseResultMapper;
import com.example.ojpt.mapper.SubmissionMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.SubmissionService;
import com.example.ojpt.vo.CodeRunCaseResultVO;
import com.example.ojpt.vo.CodeRunResultVO;
import com.example.ojpt.vo.DistributionBucketVO;
import com.example.ojpt.vo.SubmissionCreateResultVO;
import com.example.ojpt.vo.SubmissionRankStatsVO;
import com.example.ojpt.vo.UserSubmissionRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_AC = "AC";
    private static final String STATUS_WA = "WA";
    private static final String STATUS_CE = "CE";
    private static final String STATUS_RE = "RE";
    private static final String STATUS_TLE = "TLE";
    private static final String STATUS_SYSTEM_ERROR = "SYSTEM_ERROR";
    private static final String CASE_TYPE_SAMPLE = "SAMPLE";
    private static final String CASE_TYPE_HIDDEN = "HIDDEN";
    private static final String CASE_TYPE_CUSTOM = "CUSTOM";
    private static final String PROGRESS_ATTEMPTED = "ATTEMPTED";
    private static final String PROGRESS_SOLVED = "SOLVED";

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final UserProblemProgressMapper userProblemProgressMapper;
    private final ProblemTestCaseMapper problemTestCaseMapper;
    private final SubmissionCaseResultMapper submissionCaseResultMapper;
    private final CodeExecutionService codeExecutionService;

    @Override
    public CodeRunResultVO runCode(CodeRunDTO dto) {
        if (dto.getCases() == null || dto.getCases().isEmpty()) {
            throw BusinessException.badRequest("至少需要一个运行用例");
        }

        List<CodeRunCaseResultVO> caseResults = new java.util.ArrayList<>();
        for (int i = 0; i < dto.getCases().size(); i++) {
            CodeRunCaseDTO runCase = dto.getCases().get(i);
            CodeExecutionResult executionResult = codeExecutionService.execute(
                    dto.getLanguage(),
                    dto.getSourceCode(),
                    runCase.getInputText(),
                    dto.getTimeLimitMs(),
                    dto.getMemoryLimitKb()
            );
            CodeRunCaseResultVO caseResult = toRunCaseResult(i, runCase, executionResult);
            caseResults.add(caseResult);
            if (!STATUS_AC.equals(caseResult.getStatus())) {
                break;
            }
        }
        return new CodeRunResultVO("FINISHED", caseResults);
    }

    private CodeRunCaseResultVO toRunCaseResult(int index, CodeRunCaseDTO runCase, CodeExecutionResult executionResult) {
        if (!executionResult.isCompileSuccess()) {
            return new CodeRunCaseResultVO(
                    index,
                    CASE_TYPE_CUSTOM,
                    STATUS_CE,
                    runCase.getInputText(),
                    runCase.getExpectedOutput(),
                    executionResult.getStdout(),
                    executionResult.getStderr(),
                    executionResult.getTimeMs(),
                    "编译失败"
            );
        }
        if (executionResult.isTimedOut()) {
            return new CodeRunCaseResultVO(
                    index,
                    CASE_TYPE_CUSTOM,
                    STATUS_TLE,
                    runCase.getInputText(),
                    runCase.getExpectedOutput(),
                    executionResult.getStdout(),
                    executionResult.getStderr(),
                    executionResult.getTimeMs(),
                    "运行超时"
            );
        }
        if (!executionResult.isRuntimeSuccess()) {
            return new CodeRunCaseResultVO(
                    index,
                    CASE_TYPE_CUSTOM,
                    STATUS_RE,
                    runCase.getInputText(),
                    runCase.getExpectedOutput(),
                    executionResult.getStdout(),
                    executionResult.getStderr(),
                    executionResult.getTimeMs(),
                    "运行错误"
            );
        }
        boolean passed = normalizeText(runCase.getExpectedOutput()).equals(normalizeText(executionResult.getStdout()));
        return new CodeRunCaseResultVO(
                index,
                CASE_TYPE_CUSTOM,
                passed ? STATUS_AC : STATUS_WA,
                runCase.getInputText(),
                runCase.getExpectedOutput(),
                executionResult.getStdout(),
                executionResult.getStderr(),
                executionResult.getTimeMs(),
                passed ? "通过" : "输出与预期不一致"
        );
    }

    @Override
    @Transactional
    public SubmissionCreateResultVO createSubmission(Long userId, Integer problemNo, SubmissionCreateDTO dto) {
        Problem problem = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getProblemNo, problemNo)
                        .eq(Problem::getIsDeleted, 0)
                        .eq(Problem::getStatus, STATUS_PUBLISHED)
        );
        if (problem == null) {
            throw BusinessException.notFound("题目");
        }

        Submission submission = new Submission()
                .setUserId(userId)
                .setProblemId(problem.getId())
                .setLanguage(dto.getLanguage())
                .setSourceCode(dto.getSourceCode())
                .setStatus(STATUS_QUEUED)
                .setJudgeMessage("等待判题");
        submissionMapper.insert(submission);

        problem.setSubmitCount((problem.getSubmitCount() == null ? 0 : problem.getSubmitCount()) + 1);
        problemMapper.updateById(problem);

        UserProblemProgress progress = userProblemProgressMapper.selectOne(
                new LambdaQueryWrapper<UserProblemProgress>()
                        .eq(UserProblemProgress::getUserId, userId)
                        .eq(UserProblemProgress::getProblemId, problem.getId())
        );
        if (progress == null) {
            progress = new UserProblemProgress()
                    .setUserId(userId)
                    .setProblemId(problem.getId())
                    .setStatus(PROGRESS_ATTEMPTED)
                    .setLastSubmissionId(submission.getId());
            userProblemProgressMapper.insert(progress);
        } else {
            progress.setStatus(PROGRESS_ATTEMPTED);
            progress.setLastSubmissionId(submission.getId());
            userProblemProgressMapper.updateById(progress);
        }

        List<ProblemTestCase> judgeCases = new java.util.ArrayList<>(problemTestCaseMapper.selectList(
                new LambdaQueryWrapper<ProblemTestCase>()
                        .eq(ProblemTestCase::getProblemId, problem.getId())
                        .in(ProblemTestCase::getCaseType, CASE_TYPE_SAMPLE, CASE_TYPE_HIDDEN)
                        .orderByAsc(ProblemTestCase::getSortOrder)
                        .orderByAsc(ProblemTestCase::getId)
        ));
        judgeCases.sort(Comparator
                .comparingInt((ProblemTestCase item) -> CASE_TYPE_SAMPLE.equals(item.getCaseType()) ? 0 : 1)
                .thenComparing(ProblemTestCase::getSortOrder)
                .thenComparing(ProblemTestCase::getId));

        if (judgeCases.isEmpty()) {
            submission.setStatus(STATUS_SYSTEM_ERROR);
            submission.setJudgeMessage("当前题目尚未配置判题用例");
            submissionMapper.updateById(submission);
            SubmissionCreateResultVO result = new SubmissionCreateResultVO(submission.getId(), submission.getStatus(), submission.getJudgeMessage());
            result.setTotalCaseCount(0);
            return result;
        }

        submission.setStatus(STATUS_RUNNING);
        submissionMapper.updateById(submission);

        SubmissionCreateResultVO result = judgeSubmission(problem, submission, judgeCases);

        if (STATUS_AC.equals(result.getStatus())) {
            problem.setAcceptedCount((problem.getAcceptedCount() == null ? 0 : problem.getAcceptedCount()) + 1);
            problemMapper.updateById(problem);
            progress.setStatus(PROGRESS_SOLVED);
            progress.setLastSubmissionId(submission.getId());
            userProblemProgressMapper.updateById(progress);
        }

        return result;
    }

    private SubmissionCreateResultVO judgeSubmission(Problem problem, Submission submission, List<ProblemTestCase> judgeCases) {
        List<CodeRunCaseResultVO> caseResults = new java.util.ArrayList<>();
        int maxTimeMs = 0;
        String finalStatus = STATUS_AC;
        String finalMessage = "判题通过";
        String compileMessage = null;
        String judgeMessage = "判题通过";

        for (int i = 0; i < judgeCases.size(); i++) {
            ProblemTestCase judgeCase = judgeCases.get(i);
            CodeExecutionResult executionResult = codeExecutionService.execute(
                    submission.getLanguage(),
                    submission.getSourceCode(),
                    judgeCase.getInputText(),
                    problem.getTimeLimitMs(),
                    problem.getMemoryLimitKb()
            );

            CodeRunCaseResultVO caseResult = toSubmissionCaseResult(problem, i, judgeCase, executionResult);
            persistCaseResult(submission.getId(), caseResult);
            caseResults.add(sanitizeCaseResultForResponse(caseResult));

            if (caseResult.getTimeMs() != null) {
                maxTimeMs = Math.max(maxTimeMs, caseResult.getTimeMs().intValue());
            }
            if (STATUS_AC.equals(finalStatus) && !STATUS_AC.equals(caseResult.getStatus())) {
                finalStatus = caseResult.getStatus();
                finalMessage = switch (caseResult.getStatus()) {
                    case STATUS_CE -> "编译失败";
                    case STATUS_TLE -> "运行超时";
                    case STATUS_RE -> "运行错误";
                    case STATUS_WA -> "答案错误";
                    default -> "系统错误";
                };
                judgeMessage = caseResult.getMessage();
                if (STATUS_CE.equals(caseResult.getStatus())) {
                    compileMessage = caseResult.getErrorOutput();
                }
                break;
            }
        }

        submission.setStatus(finalStatus);
        submission.setTimeMs(maxTimeMs);
        submission.setCompileMessage(compileMessage);
        submission.setJudgeMessage(judgeMessage);
        submissionMapper.updateById(submission);
        Integer rank = STATUS_AC.equals(finalStatus) ? calculateAcRank(problem.getId(), maxTimeMs) : null;
        SubmissionRankStatsVO rankStats = buildRankStats(problem.getId());
        return new SubmissionCreateResultVO(
                submission.getId(),
                finalStatus,
                finalMessage,
                maxTimeMs,
                rank,
                judgeCases.size(),
                rankStats,
                caseResults
        );
    }

    private CodeRunCaseResultVO toSubmissionCaseResult(
            Problem problem,
            int index,
            ProblemTestCase judgeCase,
            CodeExecutionResult executionResult) {
        if (!executionResult.isCompileSuccess()) {
            return new CodeRunCaseResultVO(
                    index,
                    judgeCase.getCaseType(),
                    STATUS_CE,
                    judgeCase.getInputText(),
                    judgeCase.getExpectedOutput(),
                    executionResult.getStdout(),
                    executionResult.getStderr(),
                    executionResult.getTimeMs(),
                    "编译失败"
            );
        }
        if (executionResult.isTimedOut()) {
            return new CodeRunCaseResultVO(
                    index,
                    judgeCase.getCaseType(),
                    STATUS_TLE,
                    judgeCase.getInputText(),
                    judgeCase.getExpectedOutput(),
                    executionResult.getStdout(),
                    executionResult.getStderr(),
                    executionResult.getTimeMs(),
                    "运行超时"
            );
        }
        if (!executionResult.isRuntimeSuccess()) {
            return new CodeRunCaseResultVO(
                    index,
                    judgeCase.getCaseType(),
                    STATUS_RE,
                    judgeCase.getInputText(),
                    judgeCase.getExpectedOutput(),
                    executionResult.getStdout(),
                    executionResult.getStderr(),
                    executionResult.getTimeMs(),
                    "运行错误"
            );
        }
        boolean passed = matchesExpectedOutput(problem.getProblemNo(), judgeCase.getExpectedOutput(), executionResult.getStdout());
        return new CodeRunCaseResultVO(
                index,
                judgeCase.getCaseType(),
                passed ? STATUS_AC : STATUS_WA,
                judgeCase.getInputText(),
                judgeCase.getExpectedOutput(),
                executionResult.getStdout(),
                executionResult.getStderr(),
                executionResult.getTimeMs(),
                passed ? "通过" : "输出与预期不一致"
        );
    }

    private void persistCaseResult(Long submissionId, CodeRunCaseResultVO caseResult) {
        submissionCaseResultMapper.insert(new SubmissionCaseResult()
                .setSubmissionId(submissionId)
                .setCaseType(caseResult.getCaseType())
                .setCaseIndex(caseResult.getCaseIndex())
                .setInputText(caseResult.getInputText())
                .setExpectedOutput(caseResult.getExpectedOutput())
                .setActualOutput(caseResult.getActualOutput())
                .setErrorOutput(caseResult.getErrorOutput())
                .setStatus(caseResult.getStatus())
                .setTimeMs(caseResult.getTimeMs() == null ? null : caseResult.getTimeMs().intValue())
                .setMessage(caseResult.getMessage()));
    }

    private CodeRunCaseResultVO sanitizeCaseResultForResponse(CodeRunCaseResultVO caseResult) {
        if (!CASE_TYPE_HIDDEN.equals(caseResult.getCaseType())) {
            return caseResult;
        }
        return new CodeRunCaseResultVO(
                caseResult.getCaseIndex(),
                caseResult.getCaseType(),
                caseResult.getStatus(),
                null,
                null,
                null,
                null,
                caseResult.getTimeMs(),
                caseResult.getMessage()
        );
    }

    private Integer calculateAcRank(Long problemId, Integer timeMs) {
        Long fasterCount = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getProblemId, problemId)
                        .eq(Submission::getStatus, STATUS_AC)
                        .lt(Submission::getTimeMs, timeMs)
        );
        return (fasterCount == null ? 0 : fasterCount.intValue()) + 1;
    }

    private SubmissionRankStatsVO buildRankStats(Long problemId) {
        List<Submission> acceptedSubmissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getProblemId, problemId)
                        .eq(Submission::getStatus, STATUS_AC)
        );
        if (acceptedSubmissions == null) {
            acceptedSubmissions = List.of();
        }
        return new SubmissionRankStatsVO(
                acceptedSubmissions.size(),
                buildTimeBuckets(acceptedSubmissions)
        );
    }

    private List<DistributionBucketVO> buildTimeBuckets(List<Submission> submissions) {
        List<Integer> times = submissions.stream()
                .map(Submission::getTimeMs)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();
        if (times.isEmpty()) {
            return List.of();
        }
        int min = times.get(0);
        int max = times.get(times.size() - 1);
        if (min == max) {
            return List.of(new DistributionBucketVO(min + " ms", min, max, times.size()));
        }

        int bucketCount = Math.min(6, max - min + 1);
        int bucketSize = (int) Math.ceil((max - min + 1) / (double) bucketCount);
        List<DistributionBucketVO> buckets = new java.util.ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            int bucketMin = min + i * bucketSize;
            int bucketMax = bucketMin + bucketSize - 1;
            buckets.add(new DistributionBucketVO(bucketMin + "-" + bucketMax + " ms", bucketMin, bucketMax, 0));
        }
        times.forEach(time -> addToBucket(buckets, time));
        return buckets;
    }

    private void addToBucket(List<DistributionBucketVO> buckets, Integer value) {
        if (value == null) {
            return;
        }
        for (DistributionBucketVO bucket : buckets) {
            boolean gteMin = bucket.getMin() == null || value >= bucket.getMin();
            boolean lteMax = bucket.getMax() == null || value <= bucket.getMax();
            if (gteMin && lteMax) {
                bucket.setCount(bucket.getCount() + 1);
                return;
            }
        }
    }

    private boolean matchesExpectedOutput(Integer problemNo, String expectedOutput, String actualOutput) {
        String expected = normalizeText(expectedOutput);
        String actual = normalizeText(actualOutput);

        if (problemNo != null && problemNo == 1) {
            String[] expectedParts = expected.split("\\s+");
            String[] actualParts = actual.split("\\s+");
            if (expectedParts.length != 2 || actualParts.length != 2) {
                return false;
            }
            return (expectedParts[0].equals(actualParts[0]) && expectedParts[1].equals(actualParts[1]))
                    || (expectedParts[0].equals(actualParts[1]) && expectedParts[1].equals(actualParts[0]));
        }

        return expected.equals(actual);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").trim();
    }

    @Override
    public PageResult<UserSubmissionRecordVO> getCurrentUserSubmissions(Long userId, Integer page, Integer size) {
        long current = page != null && page > 0 ? page : 1;
        long pageSize = size != null && size > 0 ? size : 10;

        Page<Submission> submissionPage = submissionMapper.selectPage(
                new Page<>(current, pageSize),
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .orderByDesc(Submission::getCreatedAt)
        );

        List<Long> problemIds = submissionPage.getRecords().stream()
                .map(Submission::getProblemId)
                .distinct()
                .toList();
        Map<Long, Problem> problemMap = problemIds.isEmpty()
                ? Map.of()
                : problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity()));

        return PageResult.from(submissionPage, submission -> {
            Problem problem = problemMap.get(submission.getProblemId());
            UserSubmissionRecordVO vo = new UserSubmissionRecordVO();
            vo.setSubmissionId(submission.getId());
            vo.setProblemId(submission.getProblemId());
            vo.setProblemNo(problem != null ? problem.getProblemNo() : null);
            vo.setProblemTitle(problem != null ? problem.getTitle() : null);
            vo.setLanguage(submission.getLanguage());
            vo.setStatus(submission.getStatus());
            vo.setSourceCode(submission.getSourceCode());
            vo.setTimeMs(submission.getTimeMs());
            vo.setMemoryKb(submission.getMemoryKb());
            vo.setCompileMessage(submission.getCompileMessage());
            vo.setJudgeMessage(submission.getJudgeMessage());
            vo.setCreatedAt(submission.getCreatedAt());
            return vo;
        });
    }
}
