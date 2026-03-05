package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.dto.ProblemCreateDTO;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.ProblemService;
import com.example.ojpt.vo.ProblemDetailVO;
import com.example.ojpt.vo.ProblemListItemVO;
import com.example.ojpt.vo.ProblemSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目公开接口（创建草稿由任意登录用户发起）。
 */
@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Tag(name = "题目接口", description = "题目创建等接口（列表/详情在后续实现）")
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    @Operation(summary = "题库列表（学员端）", description = "分页查询题目列表，支持关键字、难度、标签、状态和排序，匿名可访问")
    public Result<PageResult<ProblemListItemVO>> listProblems(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orderBy", required = false) String orderBy
    ) {
        Long userId = getCurrentUserId();
        PageResult<ProblemListItemVO> pageResult = problemService.queryProblems(
                userId, page, size, keyword, difficulty, tagId, status, orderBy);
        return Result.ok(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "题目详情（学员端）", description = "获取题目详情，匿名可访问，status 依赖登录用户")
    public Result<ProblemDetailVO> getProblemDetail(@PathVariable("id") Long problemId) {
        Long userId = getCurrentUserId();
        ProblemDetailVO vo = problemService.getProblemDetail(problemId, userId);
        return Result.ok(vo);
    }

    @GetMapping("/no/{problemNo}")
    @Operation(summary = "通过题号获取题目详情（学员端）", description = "通过题号获取题目详情，匿名可访问，仅返回已发布题目")
    public Result<ProblemDetailVO> getProblemDetailByNo(@PathVariable("problemNo") Integer problemNo) {
        Long userId = getCurrentUserId();
        ProblemDetailVO vo = problemService.getProblemDetailByNo(problemNo, userId);
        return Result.ok(vo);
    }

    @PostMapping
    @Operation(summary = "创建题目草稿", description = "任意登录用户可以创建题目草稿，待管理员审核后发布到正式题库")
    public Result<ProblemSimpleVO> createProblemDraft(@Valid @RequestBody ProblemCreateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }
        ProblemSimpleVO vo = problemService.createDraft(userId, dto);
        return Result.ok("创建成功", vo);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserDetails loginUserDetails) {
            return loginUserDetails.getUserId();
        }
        if (principal instanceof Long userId) {
            return userId;
        }

        return null;
    }
}

