package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.PaginationUtils;
import com.example.ojpt.dto.ProblemCreateDTO;
import com.example.ojpt.dto.ProblemUpdateDTO;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.ProblemTag;
import com.example.ojpt.entity.Tag;
import com.example.ojpt.entity.UserProblemProgress;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.ProblemTagMapper;
import com.example.ojpt.mapper.TagMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.ProblemService;
import com.example.ojpt.vo.ProblemDetailVO;
import com.example.ojpt.vo.ProblemListItemVO;
import com.example.ojpt.vo.ProblemSimpleVO;
import com.example.ojpt.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final ProblemMapper problemMapper;
    private final TagMapper tagMapper;
    private final ProblemTagMapper problemTagMapper;
    private final UserProblemProgressMapper userProblemProgressMapper;

    @Override
    @Transactional
    public ProblemSimpleVO createDraft(Long userId, ProblemCreateDTO dto) {
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        Problem problem = new Problem();
        problem.setTitle(dto.getTitle());
        problem.setDifficulty(dto.getDifficulty());
        problem.setStatementMd(dto.getStatementMd());
        problem.setTimeLimitMs(dto.getTimeLimitMs());
        problem.setMemoryLimitKb(dto.getMemoryLimitKb());
        problem.setStatus(STATUS_DRAFT);
        problem.setSubmitCount(0L);
        problem.setAcceptedCount(0L);
        problem.setCreatedBy(userId);
        problem.setUpdatedBy(userId);

        problemMapper.insert(problem);

        return toSimpleVO(problem);
    }

    @Override
    @Transactional
    public void updateProblem(Long problemId, ProblemUpdateDTO dto) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null || Objects.equals(problem.getIsDeleted(), 1)) {
            throw BusinessException.notFound("题目不存在");
        }

        if (dto.getTitle() != null) {
            problem.setTitle(dto.getTitle());
        }
        if (dto.getDifficulty() != null) {
            problem.setDifficulty(dto.getDifficulty());
        }
        if (dto.getStatementMd() != null) {
            problem.setStatementMd(dto.getStatementMd());
        }
        if (dto.getTimeLimitMs() != null) {
            problem.setTimeLimitMs(dto.getTimeLimitMs());
        }
        if (dto.getMemoryLimitKb() != null) {
            problem.setMemoryLimitKb(dto.getMemoryLimitKb());
        }

        problemMapper.updateById(problem);
    }

    @Override
    @Transactional
    public void publishProblem(Long problemId, Long adminUserId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null || Objects.equals(problem.getIsDeleted(), 1)) {
            throw BusinessException.notFound("题目不存在");
        }
        problem.setStatus(STATUS_PUBLISHED);
        problem.setUpdatedBy(adminUserId);
        problemMapper.updateById(problem);
    }

    @Override
    @Transactional
    public void archiveProblem(Long problemId, Long adminUserId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null || Objects.equals(problem.getIsDeleted(), 1)) {
            throw BusinessException.notFound("题目不存在");
        }
        problem.setStatus(STATUS_ARCHIVED);
        problem.setUpdatedBy(adminUserId);
        problemMapper.updateById(problem);
    }

    @Override
    public ProblemSimpleVO getProblem(Long problemId) {
        Problem problem = problemMapper.selectOne(
            new LambdaQueryWrapper<Problem>()
                .eq(Problem::getId, problemId)
                .eq(Problem::getIsDeleted, 0)
        );
        if (problem == null) {
            throw BusinessException.notFound("题目不存在");
        }
        return toSimpleVO(problem);
    }

    @Override
    public PageResult<ProblemListItemVO> queryProblems(
            Long userId,
            Integer page,
            Integer size,
            String keyword,
            String difficulty,
            Long tagId,
            String status,
            String orderBy
    ) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);

        Page<Problem> pageParam = new Page<>(p, s);

        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
            .eq(Problem::getIsDeleted, 0)
            .eq(Problem::getStatus, STATUS_PUBLISHED);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Problem::getTitle, keyword.trim());
        }
        if (difficulty != null && !difficulty.isBlank()) {
            wrapper.eq(Problem::getDifficulty, difficulty.trim());
        }

        // 标签过滤：通过 problem_tag 中间表筛选 problemId
        if (tagId != null) {
            List<Long> tagProblemIds = problemTagMapper.selectList(
                    new LambdaQueryWrapper<ProblemTag>()
                        .eq(ProblemTag::getTagId, tagId)
            ).stream().map(ProblemTag::getProblemId).distinct().toList();
            if (tagProblemIds.isEmpty()) {
                return PageResult.empty(p, s);
            }
            wrapper.in(Problem::getId, tagProblemIds);
        }

        // 排序
        if ("ID".equalsIgnoreCase(orderBy)) {
            wrapper.orderByAsc(Problem::getProblemNo);
        } else if ("DIFFICULTY".equalsIgnoreCase(orderBy)) {
            wrapper.orderByAsc(Problem::getDifficulty).orderByAsc(Problem::getProblemNo);
        } else if ("ACCEPTANCE".equalsIgnoreCase(orderBy)) {
            // 通过率 = accepted / submit，简单按 acceptedCount 降序
            wrapper.orderByDesc(Problem::getAcceptedCount);
        } else {
            // 默认按题号升序
            wrapper.orderByAsc(Problem::getProblemNo);
        }

        Page<Problem> result = problemMapper.selectPage(pageParam, wrapper);
        if (result.getRecords().isEmpty()) {
            return PageResult.empty(p, s);
        }

        List<Problem> problems = result.getRecords();
        List<Long> problemIds = problems.stream().map(Problem::getId).toList();

        // 查询标签
        List<ProblemTag> problemTags = problemTagMapper.selectList(
                new LambdaQueryWrapper<ProblemTag>()
                    .in(ProblemTag::getProblemId, problemIds)
        );
        List<Long> tagIds = problemTags.stream().map(ProblemTag::getTagId).distinct().toList();
        Map<Long, Tag> tagMap = tagIds.isEmpty()
                ? Map.of()
                : tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, t -> t));

        Map<Long, List<Tag>> problemIdToTags = problemTags.stream().collect(
                Collectors.groupingBy(
                        ProblemTag::getProblemId,
                        Collectors.mapping(
                                pt -> tagMap.get(pt.getTagId()),
                                Collectors.toList()
                        )
                )
        );

        // 查询当前用户的做题状态
        Map<Long, String> statusMap;
        if (userId != null) {
            List<UserProblemProgress> progresses = userProblemProgressMapper.selectList(
                    new LambdaQueryWrapper<UserProblemProgress>()
                        .eq(UserProblemProgress::getUserId, userId)
                        .in(UserProblemProgress::getProblemId, problemIds)
            );
            statusMap = progresses.stream().collect(
                    Collectors.toMap(
                            UserProblemProgress::getProblemId,
                            UserProblemProgress::getStatus,
                            (a, b) -> b
                    )
            );
        } else {
            statusMap = Map.of();
        }

        // 根据 status 参数再过滤一层（仅对已登录用户生效）
        final List<Problem> filteredProblems;
        if (userId != null && status != null && !status.isBlank()) {
            String expected = status.trim();
            filteredProblems = problems.stream()
                    .filter(p2 -> expected.equals(statusMap.get(p2.getId())))
                    .toList();
        } else {
            filteredProblems = problems;
        }

        List<ProblemListItemVO> listVos = filteredProblems.stream()
                .map(p2 -> {
                    ProblemListItemVO vo = new ProblemListItemVO();
                    vo.setId(p2.getId());
                    vo.setProblemNo(p2.getProblemNo());
                    vo.setTitle(p2.getTitle());
                    vo.setDifficulty(p2.getDifficulty());
                    vo.setStatus(statusMap.get(p2.getId()));
                    vo.setSubmitCount(p2.getSubmitCount());
                    vo.setAcceptedCount(p2.getAcceptedCount());
                    if (p2.getSubmitCount() != null && p2.getSubmitCount() > 0) {
                        double rate = (double) (p2.getAcceptedCount() == null ? 0 : p2.getAcceptedCount())
                                / p2.getSubmitCount() * 100.0;
                        vo.setAcceptanceRate(Math.round(rate * 10.0) / 10.0);
                    }
                    List<TagVO> tags = problemIdToTags.getOrDefault(p2.getId(), List.of()).stream()
                            .filter(Objects::nonNull)
                            .map(t -> {
                                TagVO tvo = new TagVO();
                                tvo.setId(t.getId());
                                tvo.setName(t.getName());
                                tvo.setType(t.getType());
                                return tvo;
                            })
                            .toList();
                    vo.setTags(tags);
                    return vo;
                })
                .toList();

        return PageResult.of(listVos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ProblemDetailVO getProblemDetail(Long problemId, Long userId) {
        Problem problem = problemMapper.selectOne(
                new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getId, problemId)
                    .eq(Problem::getIsDeleted, 0)
        );
        if (problem == null || STATUS_ARCHIVED.equals(problem.getStatus())) {
            throw BusinessException.notFound("题目");
        }

        // 标签
        List<ProblemTag> problemTags = problemTagMapper.selectList(
                new LambdaQueryWrapper<ProblemTag>()
                    .eq(ProblemTag::getProblemId, problemId)
        );
        List<Long> tagIds = problemTags.stream().map(ProblemTag::getTagId).distinct().toList();
        List<TagVO> tagVos = tagIds.isEmpty()
                ? List.of()
                : tagMapper.selectBatchIds(tagIds).stream()
                    .map(t -> {
                        TagVO vo = new TagVO();
                        vo.setId(t.getId());
                        vo.setName(t.getName());
                        vo.setType(t.getType());
                        return vo;
                    })
                    .toList();

        // 当前用户状态
        String progressStatus = null;
        if (userId != null) {
            UserProblemProgress progress = userProblemProgressMapper.selectOne(
                    new LambdaQueryWrapper<UserProblemProgress>()
                        .eq(UserProblemProgress::getUserId, userId)
                        .eq(UserProblemProgress::getProblemId, problemId)
            );
            if (progress != null) {
                progressStatus = progress.getStatus();
            }
        }

        ProblemDetailVO vo = new ProblemDetailVO();
        vo.setId(problem.getId());
        vo.setProblemNo(problem.getProblemNo());
        vo.setTitle(problem.getTitle());
        vo.setDifficulty(problem.getDifficulty());
        vo.setStatus(progressStatus);
        vo.setStatementMd(problem.getStatementMd());
        vo.setTimeLimitMs(problem.getTimeLimitMs());
        vo.setMemoryLimitKb(problem.getMemoryLimitKb());
        vo.setSubmitCount(problem.getSubmitCount());
        vo.setAcceptedCount(problem.getAcceptedCount());
        if (problem.getSubmitCount() != null && problem.getSubmitCount() > 0) {
            double rate = (double) (problem.getAcceptedCount() == null ? 0 : problem.getAcceptedCount())
                    / problem.getSubmitCount() * 100.0;
            vo.setAcceptanceRate(Math.round(rate * 10.0) / 10.0);
        }
        vo.setTags(tagVos);
        vo.setCreatedAt(problem.getCreatedAt());
        vo.setUpdatedAt(problem.getUpdatedAt());
        return vo;
    }

    @Override
    public ProblemDetailVO getProblemDetailByNo(Integer problemNo, Long userId) {
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

        // 复用原详情逻辑：需要按 ID 查询标签与进度
        return getProblemDetail(problem.getId(), userId);
    }

    private ProblemSimpleVO toSimpleVO(Problem problem) {
        ProblemSimpleVO vo = new ProblemSimpleVO();
        vo.setId(problem.getId());
        vo.setProblemNo(problem.getProblemNo());
        vo.setTitle(problem.getTitle());
        vo.setDifficulty(problem.getDifficulty());
        vo.setStatus(problem.getStatus());
        vo.setSubmitCount(problem.getSubmitCount());
        vo.setAcceptedCount(problem.getAcceptedCount());
        vo.setCreatedAt(problem.getCreatedAt());
        vo.setUpdatedAt(problem.getUpdatedAt());
        return vo;
    }
}

