package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.ProblemService;
import com.example.ojpt.vo.ProblemDetailVO;
import com.example.ojpt.vo.ProblemListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Tag(name = "题目接口", description = "精简版题库浏览接口")
public class ProblemController {

    private final ProblemService problemService;

    @GetMapping
    @Operation(summary = "题库列表")
    public Result<PageResult<ProblemListItemVO>> listProblems(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orderBy", required = false) String orderBy) {
        PageResult<ProblemListItemVO> pageResult = problemService.queryProblems(
                getCurrentUserId(), page, size, keyword, difficulty, tagId, status, orderBy);
        return Result.ok(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "题目详情")
    public Result<ProblemDetailVO> getProblemDetail(@PathVariable("id") Long problemId) {
        return Result.ok(problemService.getProblemDetail(problemId, getCurrentUserId()));
    }

    @GetMapping("/no/{problemNo}")
    @Operation(summary = "按题号获取题目详情")
    public Result<ProblemDetailVO> getProblemDetailByNo(@PathVariable("problemNo") Integer problemNo) {
        return Result.ok(problemService.getProblemDetailByNo(problemNo, getCurrentUserId()));
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
