package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.dto.SubmissionCreateDTO;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.SubmissionService;
import com.example.ojpt.vo.SubmissionVO;
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
 * 学员端提交接口。
 */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "提交接口", description = "提交代码与查看提交记录接口")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @Operation(summary = "创建提交", description = "提交代码进行判题（stub 阶段仅记录 QUEUED 状态）")
    public Result<SubmissionVO> createSubmission(@Valid @RequestBody SubmissionCreateDTO dto) {
        Long userId = getCurrentUserIdOrThrow();
        SubmissionVO vo = submissionService.createSubmission(userId, dto);
        return Result.ok("提交成功", vo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询提交详情", description = "查询单个提交详情，普通用户仅可查看自己的提交")
    public Result<SubmissionVO> getSubmission(@PathVariable("id") Long submissionId) {
        Long userId = getCurrentUserIdOrThrow();
        SubmissionVO vo = submissionService.getSubmission(userId, submissionId, false);
        return Result.ok(vo);
    }

    @GetMapping
    @Operation(summary = "我的提交列表", description = "查询当前用户的提交列表，可按题目过滤")
    public Result<PageResult<SubmissionVO>> listMySubmissions(
            @RequestParam(value = "problemId", required = false) Long problemId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size
    ) {
        Long userId = getCurrentUserIdOrThrow();
        PageResult<SubmissionVO> result = submissionService.listMySubmissions(userId, problemId, page, size);
        return Result.ok(result);
    }

    private Long getCurrentUserIdOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserDetails loginUserDetails) {
            return loginUserDetails.getUserId();
        }
        if (principal instanceof Long userId) {
            // JwtAuthenticationFilter 默认将 principal 设置为 userId（Long）
            return userId;
        }

        throw BusinessException.unauthorized("无法获取用户ID");
    }
}

