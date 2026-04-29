package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.dto.CodeRunDTO;
import com.example.ojpt.dto.ProblemCodeDraftSaveDTO;
import com.example.ojpt.dto.SubmissionCreateDTO;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.ProblemCodeDraftService;
import com.example.ojpt.service.ProblemService;
import com.example.ojpt.service.ProblemTestCaseService;
import com.example.ojpt.service.SubmissionService;
import com.example.ojpt.vo.ProblemCodeDraftVO;
import com.example.ojpt.vo.ProblemDetailVO;
import com.example.ojpt.vo.ProblemListItemVO;
import com.example.ojpt.vo.ProblemTestCaseVO;
import com.example.ojpt.vo.CodeRunResultVO;
import com.example.ojpt.vo.SubmissionCreateResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Tag(name = "题目接口", description = "题库浏览、样例测试用例与代码提交接口")
public class ProblemController {

    private final ProblemService problemService;
    private final ProblemTestCaseService problemTestCaseService;
    private final SubmissionService submissionService;
    private final ProblemCodeDraftService problemCodeDraftService;

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

    @GetMapping("/no/{problemNo}/test-cases/sample")
    @Operation(summary = "按题号获取公开样例测试用例")
    public Result<List<ProblemTestCaseVO>> getProblemSampleTestCases(@PathVariable("problemNo") Integer problemNo) {
        return Result.ok(problemTestCaseService.getSampleTestCasesByProblemNo(problemNo));
    }

    @GetMapping("/no/{problemNo}/draft")
    @Operation(summary = "获取当前用户题目代码草稿")
    public Result<ProblemCodeDraftVO> getCodeDraft(
            @PathVariable("problemNo") Integer problemNo,
            @RequestParam("language") String language) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw com.example.ojpt.exception.BusinessException.unauthorized("未登录");
        }
        return Result.ok(problemCodeDraftService.getDraft(userId, problemNo, language));
    }

    @PutMapping("/no/{problemNo}/draft")
    @Operation(summary = "保存当前用户题目代码草稿")
    public Result<ProblemCodeDraftVO> saveCodeDraft(
            @PathVariable("problemNo") Integer problemNo,
            @Valid @RequestBody ProblemCodeDraftSaveDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw com.example.ojpt.exception.BusinessException.unauthorized("未登录");
        }
        return Result.ok(problemCodeDraftService.saveDraft(userId, problemNo, dto));
    }

    @PostMapping("/no/{problemNo}/submissions")
    @Operation(summary = "提交代码", description = "按题号提交代码，当前版本先记录提交并进入等待判题状态")
    public Result<SubmissionCreateResultVO> submitCode(
            @PathVariable("problemNo") Integer problemNo,
            @Valid @RequestBody SubmissionCreateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw com.example.ojpt.exception.BusinessException.unauthorized("未登录");
        }
        return Result.ok(submissionService.createSubmission(userId, problemNo, dto));
    }

    @GetMapping("/submissions/{submissionId}")
    @Operation(summary = "鏌ヨ鎻愪氦缁撴灉", description = "鏍规嵁 submissionId 鏌ヨ褰撳墠鐢ㄦ埛鐨勬彁浜ょ姸鎬佷笌鍒ら缁撴灉")
    public Result<SubmissionCreateResultVO> getSubmissionResult(@PathVariable("submissionId") Long submissionId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw com.example.ojpt.exception.BusinessException.unauthorized("鏈櫥褰?");
        }
        return Result.ok(submissionService.getSubmissionResult(userId, submissionId));
    }

    @PostMapping("/run")
    @Operation(summary = "运行代码", description = "使用页面传入的公开样例或自定义用例同步运行代码")
    public Result<CodeRunResultVO> runCode(@Valid @RequestBody CodeRunDTO dto) {
        return Result.ok(submissionService.runCode(dto));
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
