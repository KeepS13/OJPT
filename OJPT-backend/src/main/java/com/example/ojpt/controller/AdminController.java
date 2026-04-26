package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.dto.ProblemUpdateDTO;
import com.example.ojpt.dto.TagCreateDTO;
import com.example.ojpt.dto.TagUpdateDTO;
import com.example.ojpt.dto.UserUpdateDTO;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.AdminService;
import com.example.ojpt.service.ProblemService;
import com.example.ojpt.service.TagService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.AdminProblemListItemVO;
import com.example.ojpt.vo.ProblemSimpleVO;
import com.example.ojpt.vo.StatisticsVO;
import com.example.ojpt.vo.TagVO;
import com.example.ojpt.vo.UserDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "管理员接口", description = "精简版管理员接口，仅保留用户、题目、标签与统计能力")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final ProblemService problemService;
    private final TagService tagService;

    @GetMapping("/users")
    @Operation(summary = "获取用户列表")
    public Result<PageResult<UserDetailVO>> getUsers(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String roleType,
            @RequestParam(required = false) String keyword) {
        PageResult<UserDetailVO> users = adminService.getUsers(page, size, status, roleType, keyword);
        return Result.ok(users);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "获取用户详情")
    public Result<UserDetailVO> getUser(@PathVariable Long userId) {
        return Result.ok(adminService.getUser(userId));
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateDTO dto) {
        adminService.updateUser(userId, dto);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "删除用户")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return Result.ok("删除成功");
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "更新用户状态")
    public Result<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Integer> request) {
        if (request == null || !request.containsKey("status")) {
            throw BusinessException.badRequest("请求体中必须包含 status 字段");
        }

        Integer status = request.get("status");
        if (status == null) {
            throw BusinessException.badRequest("status 字段不能为空");
        }
        if (status < 0 || status > 2) {
            throw BusinessException.badRequest("status 值必须在 0-2 之间");
        }

        adminService.updateUserStatus(userId, status);
        return Result.ok("状态更新成功");
    }

    @GetMapping("/problems")
    @Operation(summary = "管理员题目列表")
    public Result<PageResult<AdminProblemListItemVO>> listAdminProblems(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orderBy", required = false) String orderBy) {
        PageResult<AdminProblemListItemVO> pageResult =
                problemService.queryAdminProblems(page, size, keyword, difficulty, tagId, status, orderBy);
        return Result.ok(pageResult);
    }

    @GetMapping("/problems/{problemId}")
    @Operation(summary = "获取题目详情")
    public Result<ProblemSimpleVO> getProblem(@PathVariable Long problemId) {
        return Result.ok(problemService.getProblem(problemId));
    }

    @PutMapping("/problems/{problemId}")
    @Operation(summary = "更新题目")
    public Result<Void> updateProblem(@PathVariable Long problemId, @Valid @RequestBody ProblemUpdateDTO dto) {
        problemService.updateProblem(problemId, dto);
        return Result.ok("更新成功");
    }

    @PostMapping("/problems/{problemId}:publish")
    @Operation(summary = "发布题目")
    public Result<Void> publishProblem(@PathVariable Long problemId) {
        problemService.publishProblem(problemId, getCurrentUserId());
        return Result.ok("发布成功");
    }

    @PostMapping("/problems/{problemId}:archive")
    @Operation(summary = "归档题目")
    public Result<Void> archiveProblem(@PathVariable Long problemId) {
        problemService.archiveProblem(problemId, getCurrentUserId());
        return Result.ok("归档成功");
    }

    @GetMapping("/tags")
    @Operation(summary = "获取标签列表")
    public Result<List<TagVO>> getTags() {
        return Result.ok(tagService.listAll());
    }

    @PostMapping("/tags")
    @Operation(summary = "创建标签")
    public Result<TagVO> createTag(@Valid @RequestBody TagCreateDTO dto) {
        return Result.ok("创建成功", tagService.createTag(dto));
    }

    @PutMapping("/tags/{tagId}")
    @Operation(summary = "更新标签")
    public Result<Void> updateTag(@PathVariable Long tagId, @Valid @RequestBody TagUpdateDTO dto) {
        tagService.updateTag(tagId, dto);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/tags/{tagId}")
    @Operation(summary = "删除标签")
    public Result<Void> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return Result.ok("删除成功");
    }

    @PostMapping("/problems/{problemId}/tags")
    @Operation(summary = "为题目绑定标签")
    public Result<Void> addTagToProblem(@PathVariable Long problemId, @RequestParam("tagId") Long tagId) {
        tagService.addTagToProblem(problemId, tagId);
        return Result.ok("绑定成功");
    }

    @DeleteMapping("/problems/{problemId}/tags")
    @Operation(summary = "移除题目标签")
    public Result<Void> removeTagFromProblem(@PathVariable Long problemId, @RequestParam("tagId") Long tagId) {
        tagService.removeTagFromProblem(problemId, tagId);
        return Result.ok("移除成功");
    }

    @GetMapping("/statistics/overview")
    @Operation(summary = "获取平台概览统计")
    public Result<StatisticsVO> getOverviewStatistics() {
        return Result.ok(adminService.getOverviewStatistics());
    }

    @GetMapping("/statistics/users")
    @Operation(summary = "获取用户统计")
    public Result<StatisticsVO> getUserStatistics() {
        return Result.ok(adminService.getUserStatistics());
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw BusinessException.unauthorized("未登录");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserDetails loginUserDetails) {
            return loginUserDetails.getUserId();
        }
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String username) {
            var user = userService.findByUsername(username);
            return user != null ? user.getId() : null;
        }

        throw BusinessException.unauthorized("无法获取当前用户");
    }
}
