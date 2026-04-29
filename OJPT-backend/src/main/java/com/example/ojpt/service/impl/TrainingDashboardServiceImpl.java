package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.Submission;
import com.example.ojpt.entity.UserProblemProgress;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.SubmissionMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.TrainingDashboardService;
import com.example.ojpt.vo.training.dashboard.TrainingDashboardRecentSubmissionVO;
import com.example.ojpt.vo.training.dashboard.UserTrainingDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingDashboardServiceImpl implements TrainingDashboardService {

    private static final String STATUS_ACCEPTED = "AC";
    private static final String STATUS_SOLVED = "SOLVED";
    private static final int RECENT_SUBMISSION_LIMIT = 5;
    private static final List<String> STATUS_ORDER = List.of(
            STATUS_ACCEPTED,
            "WA",
            "TLE",
            "MLE",
            "RE",
            "CE",
            "SYSTEM_ERROR",
            "RUNNING",
            "QUEUED"
    );
    private static final List<String> DIFFICULTY_ORDER = List.of("EASY", "MEDIUM", "HARD");

    private final SubmissionMapper submissionMapper;
    private final UserProblemProgressMapper userProblemProgressMapper;
    private final ProblemMapper problemMapper;

    @Override
    public UserTrainingDashboardVO getTrainingDashboard(Long userId) {
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        List<Object> rawStatuses = submissionMapper.selectObjs(new QueryWrapper<Submission>()
                .select("status")
                .eq("user_id", userId));
        rawStatuses = rawStatuses == null ? List.of() : rawStatuses;
        Map<String, Long> statusDistribution = buildOrderedDistribution(rawStatuses, STATUS_ORDER);
        long totalSubmissions = rawStatuses.size();
        long acceptedSubmissions = statusDistribution.getOrDefault(STATUS_ACCEPTED, 0L);

        List<Submission> recentSubmissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, userId)
                .orderByDesc(Submission::getCreatedAt)
                .last("LIMIT " + RECENT_SUBMISSION_LIMIT));
        recentSubmissions = recentSubmissions == null ? List.of() : recentSubmissions;

        List<UserProblemProgress> solvedProgresses = userProblemProgressMapper.selectList(new LambdaQueryWrapper<UserProblemProgress>()
                .eq(UserProblemProgress::getUserId, userId)
                .eq(UserProblemProgress::getStatus, STATUS_SOLVED));
        solvedProgresses = solvedProgresses == null ? List.of() : solvedProgresses;

        Map<Long, Problem> problemMap = loadRelatedProblems(recentSubmissions, solvedProgresses);

        UserTrainingDashboardVO vo = new UserTrainingDashboardVO();
        vo.setTotalSubmissions(totalSubmissions);
        vo.setAcceptedSubmissions(acceptedSubmissions);
        vo.setSolvedProblemCount((long) solvedProgresses.size());
        vo.setAcceptanceRate(calculateRate(acceptedSubmissions, totalSubmissions));
        vo.setRecentSubmissions(recentSubmissions.stream()
                .map(submission -> toRecentSubmissionVO(submission, problemMap.get(submission.getProblemId())))
                .toList());
        vo.setStatusDistribution(statusDistribution);
        vo.setDifficultyDistribution(buildDifficultyDistribution(solvedProgresses, problemMap));
        return vo;
    }

    private Map<Long, Problem> loadRelatedProblems(
            List<Submission> recentSubmissions,
            List<UserProblemProgress> solvedProgresses
    ) {
        Set<Long> problemIds = new LinkedHashSet<>();
        if (recentSubmissions != null) {
            recentSubmissions.stream()
                    .map(Submission::getProblemId)
                    .filter(Objects::nonNull)
                    .forEach(problemIds::add);
        }
        if (solvedProgresses != null) {
            solvedProgresses.stream()
                    .map(UserProblemProgress::getProblemId)
                    .filter(Objects::nonNull)
                    .forEach(problemIds::add);
        }

        if (problemIds.isEmpty()) {
            return Map.of();
        }

        List<Problem> problems = problemMapper.selectBatchIds(problemIds);
        if (problems == null || problems.isEmpty()) {
            return Map.of();
        }

        return problems.stream()
                .collect(Collectors.toMap(Problem::getId, Function.identity(), (left, right) -> left));
    }

    private Map<String, Long> buildDifficultyDistribution(
            List<UserProblemProgress> solvedProgresses,
            Map<Long, Problem> problemMap
    ) {
        if (solvedProgresses == null || solvedProgresses.isEmpty()) {
            return Map.of();
        }

        List<String> difficulties = solvedProgresses.stream()
                .map(UserProblemProgress::getProblemId)
                .map(problemMap::get)
                .filter(Objects::nonNull)
                .map(problem -> normalizeLabel(problem.getDifficulty(), "UNKNOWN"))
                .toList();
        return buildOrderedDistribution(difficulties, DIFFICULTY_ORDER);
    }

    private Map<String, Long> buildOrderedDistribution(Collection<?> values, List<String> preferredOrder) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> counts = values.stream()
                .map(value -> normalizeLabel(value == null ? null : String.valueOf(value), "UNKNOWN"))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        LinkedHashMap<String, Long> ordered = new LinkedHashMap<>();
        for (String key : preferredOrder) {
            Long count = counts.remove(key);
            if (count != null && count > 0) {
                ordered.put(key, count);
            }
        }

        List<String> extras = new ArrayList<>(counts.keySet());
        extras.sort(String::compareTo);
        for (String key : extras) {
            Long count = counts.get(key);
            if (count != null && count > 0) {
                ordered.put(key, count);
            }
        }
        return ordered;
    }

    private TrainingDashboardRecentSubmissionVO toRecentSubmissionVO(Submission submission, Problem problem) {
        TrainingDashboardRecentSubmissionVO vo = new TrainingDashboardRecentSubmissionVO();
        vo.setSubmissionId(submission.getId());
        vo.setProblemId(submission.getProblemId());
        vo.setProblemNo(problem != null ? problem.getProblemNo() : null);
        vo.setProblemTitle(problem != null ? problem.getTitle() : null);
        vo.setLanguage(submission.getLanguage());
        vo.setStatus(submission.getStatus());
        vo.setTimeMs(submission.getTimeMs());
        vo.setMemoryKb(submission.getMemoryKb());
        vo.setCreatedAt(submission.getCreatedAt());
        return vo;
    }

    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((double) numerator * 1000 / denominator) / 10.0;
    }

    private String normalizeLabel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase();
    }
}
