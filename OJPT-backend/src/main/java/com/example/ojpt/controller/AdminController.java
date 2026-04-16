package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.dto.PermissionCreateDTO;
import com.example.ojpt.dto.PermissionUpdateDTO;
import com.example.ojpt.dto.ProblemUpdateDTO;
import com.example.ojpt.dto.RoleCreateDTO;
import com.example.ojpt.dto.RolePermissionAssignDTO;
import com.example.ojpt.dto.RoleUpdateDTO;
import com.example.ojpt.dto.SchoolCreateDTO;
import com.example.ojpt.dto.SchoolUpdateDTO;
import com.example.ojpt.dto.SubmissionStatusUpdateDTO;
import com.example.ojpt.dto.TagCreateDTO;
import com.example.ojpt.dto.TagUpdateDTO;
import com.example.ojpt.dto.UserRoleUpdateDTO;
import com.example.ojpt.dto.UserUpdateDTO;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.AdminService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.service.ProblemService;
import com.example.ojpt.service.TagService;
import com.example.ojpt.service.SubmissionService;
import com.example.ojpt.vo.AdminProblemListItemVO;
import com.example.ojpt.vo.PermissionVO;
import com.example.ojpt.vo.ProblemSimpleVO;
import com.example.ojpt.vo.RoleVO;
import com.example.ojpt.vo.SchoolVO;
import com.example.ojpt.vo.StatisticsVO;
import com.example.ojpt.vo.TagVO;
import com.example.ojpt.vo.UserDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "管理员接口", description = "管理员相关操作接口")
public class AdminController {
    
    private final AdminService adminService;
    private final UserService userService;
    private final ProblemService problemService;
    private final TagService tagService;
    private final SubmissionService submissionService;
    
    // 用户管理扩展
    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "分页查询用户列表，支持按状态、角色类型、关键词筛选")
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
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    public Result<UserDetailVO> getUser(@PathVariable Long userId) {
        UserDetailVO vo = adminService.getUser(userId);
        return Result.ok(vo);
    }
    
    @PutMapping("/users/{userId}")
    @Operation(summary = "更新用户信息", description = "更新指定用户的基本信息")
    public Result<Void> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateDTO dto) {
        adminService.updateUser(userId, dto);
        return Result.ok("更新成功");
    }
    
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "删除用户", description = "软删除指定用户")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return Result.ok("删除成功");
    }
    
    @PutMapping("/users/{userId}/status")
    @Operation(summary = "更新用户状态", description = "更新用户状态：0禁用/1启用/2待审核")
    public Result<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody java.util.Map<String, Integer> request) {
        if (request == null || !request.containsKey("status")) {
            throw BusinessException.badRequest("请求体中必须包含 status 字段");
        }
        Integer status = request.get("status");
        if (status == null) {
            throw BusinessException.badRequest("status 字段不能为 null");
        }
        if (status < 0 || status > 2) {
            throw BusinessException.badRequest("status 值必须在 0-2 之间（0禁用/1启用/2待审核）");
        }
        adminService.updateUserStatus(userId, status);
        return Result.ok("状态更新成功");
    }
    
    @PutMapping("/users/{userId}/roles")
    @Operation(summary = "更新用户角色", description = "更新指定用户的角色列表")
    public Result<Void> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateDTO dto) {
        adminService.updateUserRoles(userId, dto);
        return Result.ok("角色更新成功");
    }
    
    // 角色权限管理
    @GetMapping("/roles")
    @Operation(summary = "获取角色列表", description = "获取所有角色列表")
    public Result<List<RoleVO>> getRoles() {
        List<RoleVO> roles = adminService.getRoles();
        return Result.ok(roles);
    }
    
    @PostMapping("/roles")
    @Operation(summary = "创建角色", description = "创建新的角色")
    public Result<RoleVO> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        RoleVO vo = adminService.createRole(dto);
        return Result.ok("创建成功", vo);
    }
    
    @GetMapping("/roles/{roleId}")
    @Operation(summary = "获取角色详情", description = "根据角色ID获取角色详细信息")
    public Result<RoleVO> getRole(@PathVariable Long roleId) {
        RoleVO vo = adminService.getRole(roleId);
        return Result.ok(vo);
    }
    
    @PutMapping("/roles/{roleId}")
    @Operation(summary = "更新角色", description = "更新指定角色的信息")
    public Result<Void> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleUpdateDTO dto) {
        adminService.updateRole(roleId, dto);
        return Result.ok("更新成功");
    }
    
    @DeleteMapping("/roles/{roleId}")
    @Operation(summary = "删除角色", description = "删除指定角色（需确保无用户绑定）")
    public Result<Void> deleteRole(@PathVariable Long roleId) {
        adminService.deleteRole(roleId);
        return Result.ok("删除成功");
    }
    
    @GetMapping("/permissions")
    @Operation(summary = "获取权限列表", description = "获取权限列表，支持按资源、操作、关键词筛选")
    public Result<List<PermissionVO>> getPermissions(
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword) {
        List<PermissionVO> permissions = adminService.getPermissions(resource, action, keyword);
        return Result.ok(permissions);
    }
    
    @PostMapping("/permissions")
    @Operation(summary = "创建权限", description = "创建新的权限")
    public Result<PermissionVO> createPermission(@Valid @RequestBody PermissionCreateDTO dto) {
        PermissionVO vo = adminService.createPermission(dto);
        return Result.ok("创建成功", vo);
    }
    
    @GetMapping("/permissions/{permissionId}")
    @Operation(summary = "获取权限详情", description = "根据权限ID获取权限详细信息")
    public Result<PermissionVO> getPermission(@PathVariable Long permissionId) {
        PermissionVO vo = adminService.getPermission(permissionId);
        return Result.ok(vo);
    }
    
    @PutMapping("/permissions/{permissionId}")
    @Operation(summary = "更新权限", description = "更新指定权限的信息")
    public Result<Void> updatePermission(
            @PathVariable Long permissionId,
            @Valid @RequestBody PermissionUpdateDTO dto) {
        adminService.updatePermission(permissionId, dto);
        return Result.ok("更新成功");
    }
    
    @DeleteMapping("/permissions/{permissionId}")
    @Operation(summary = "删除权限", description = "删除指定权限（需确保无角色使用）")
    public Result<Void> deletePermission(@PathVariable Long permissionId) {
        adminService.deletePermission(permissionId);
        return Result.ok("删除成功");
    }
    
    @PostMapping("/roles/{roleId}/permissions")
    @Operation(summary = "为角色分配权限", description = "为指定角色分配权限列表")
    public Result<Void> assignPermissionsToRole(
            @PathVariable Long roleId,
            @Valid @RequestBody RolePermissionAssignDTO dto) {
        adminService.assignPermissionsToRole(roleId, dto);
        return Result.ok("分配成功");
    }
    
    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    @Operation(summary = "移除角色权限", description = "从指定角色中移除指定权限")
    public Result<Void> removePermissionFromRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        adminService.removePermissionFromRole(roleId, permissionId);
        return Result.ok("移除成功");
    }
    
    // 学校管理
    @GetMapping("/schools")
    @Operation(summary = "获取学校列表", description = "分页查询学校列表，支持按状态、关键词筛选")
    public Result<PageResult<SchoolVO>> getSchools(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        PageResult<SchoolVO> schools = adminService.getSchools(page, size, status, keyword);
        return Result.ok(schools);
    }
    
    @PostMapping("/schools")
    @Operation(summary = "创建学校", description = "创建新的学校")
    public Result<SchoolVO> createSchool(@Valid @RequestBody SchoolCreateDTO dto) {
        SchoolVO vo = adminService.createSchool(dto);
        return Result.ok("创建成功", vo);
    }
    
    @GetMapping("/schools/{schoolId}")
    @Operation(summary = "获取学校详情", description = "根据学校ID获取学校详细信息")
    public Result<SchoolVO> getSchool(@PathVariable Long schoolId) {
        SchoolVO vo = adminService.getSchool(schoolId);
        return Result.ok(vo);
    }
    
    @PutMapping("/schools/{schoolId}")
    @Operation(summary = "更新学校信息", description = "更新指定学校的信息")
    public Result<Void> updateSchool(
            @PathVariable Long schoolId,
            @Valid @RequestBody SchoolUpdateDTO dto) {
        adminService.updateSchool(schoolId, dto);
        return Result.ok("更新成功");
    }
    
    @DeleteMapping("/schools/{schoolId}")
    @Operation(summary = "删除学校", description = "删除指定学校")
    public Result<Void> deleteSchool(@PathVariable Long schoolId) {
        adminService.deleteSchool(schoolId);
        return Result.ok("删除成功");
    }
    
    @PutMapping("/schools/{schoolId}/status")
    @Operation(summary = "更新学校状态", description = "更新学校状态：0禁用/1启用/2待认证")
    public Result<Void> updateSchoolStatus(
            @PathVariable Long schoolId,
            @RequestBody java.util.Map<String, Integer> request) {
        if (request == null || !request.containsKey("status")) {
            throw BusinessException.badRequest("请求体中必须包含 status 字段");
        }
        Integer status = request.get("status");
        if (status == null) {
            throw BusinessException.badRequest("status 字段不能为 null");
        }
        if (status < 0 || status > 2) {
            throw BusinessException.badRequest("status 值必须在 0-2 之间（0禁用/1启用/2待认证）");
        }
        adminService.updateSchoolStatus(schoolId, status);
        return Result.ok("状态更新成功");
    }

    // 题库管理（仅 ADMIN）

    @GetMapping("/problems")
    @Operation(summary = "管理端题目列表（草稿池）", description = "分页查询题目列表，支持关键字、难度、标签、状态和排序")
    public Result<PageResult<AdminProblemListItemVO>> listAdminProblems(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "tagId", required = false) Long tagId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orderBy", required = false) String orderBy
    ) {
        PageResult<AdminProblemListItemVO> pageResult =
                problemService.queryAdminProblems(page, size, keyword, difficulty, tagId, status, orderBy);
        return Result.ok(pageResult);
    }

    @GetMapping("/problems/{problemId}")
    @Operation(summary = "获取题目详情（管理端）", description = "根据题目ID获取题目基础信息")
    public Result<ProblemSimpleVO> getProblem(@PathVariable Long problemId) {
        ProblemSimpleVO vo = problemService.getProblem(problemId);
        return Result.ok(vo);
    }

    @PutMapping("/problems/{problemId}")
    @Operation(summary = "更新题目（管理端）", description = "管理员更新题目信息")
    public Result<Void> updateProblem(
            @PathVariable Long problemId,
            @Valid @RequestBody ProblemUpdateDTO dto) {
        problemService.updateProblem(problemId, dto);
        return Result.ok("更新成功");
    }

    @PostMapping("/problems/{problemId}:publish")
    @Operation(summary = "发布题目", description = "管理员审核并发布题目到正式题库")
    public Result<Void> publishProblem(@PathVariable Long problemId) {
        Long adminUserId = getCurrentUserId();
        problemService.publishProblem(problemId, adminUserId);
        return Result.ok("发布成功");
    }

    @PostMapping("/problems/{problemId}:archive")
    @Operation(summary = "归档题目", description = "管理员将题目下架/归档")
    public Result<Void> archiveProblem(@PathVariable Long problemId) {
        Long adminUserId = getCurrentUserId();
        problemService.archiveProblem(problemId, adminUserId);
        return Result.ok("归档成功");
    }

    // 标签管理

    @GetMapping("/tags")
    @Operation(summary = "获取标签列表", description = "获取所有题目标签")
    public Result<java.util.List<TagVO>> getTags() {
        java.util.List<TagVO> tags = tagService.listAll();
        return Result.ok(tags);
    }

    @PostMapping("/tags")
    @Operation(summary = "创建标签", description = "创建新的题目标签")
    public Result<TagVO> createTag(@Valid @RequestBody TagCreateDTO dto) {
        TagVO vo = tagService.createTag(dto);
        return Result.ok("创建成功", vo);
    }

    @PutMapping("/tags/{tagId}")
    @Operation(summary = "更新标签", description = "更新题目标签信息")
    public Result<Void> updateTag(
            @PathVariable Long tagId,
            @Valid @RequestBody TagUpdateDTO dto) {
        tagService.updateTag(tagId, dto);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/tags/{tagId}")
    @Operation(summary = "删除标签", description = "删除题目标签（会同时移除关联）")
    public Result<Void> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return Result.ok("删除成功");
    }

    @PostMapping("/problems/{problemId}/tags")
    @Operation(summary = "为题目绑定标签", description = "为指定题目绑定标签")
    public Result<Void> addTagToProblem(
            @PathVariable Long problemId,
            @RequestParam("tagId") Long tagId) {
        tagService.addTagToProblem(problemId, tagId);
        return Result.ok("绑定成功");
    }

    @DeleteMapping("/problems/{problemId}/tags")
    @Operation(summary = "移除题目标签", description = "从题目中移除指定标签")
    public Result<Void> removeTagFromProblem(
            @PathVariable Long problemId,
            @RequestParam("tagId") Long tagId) {
        tagService.removeTagFromProblem(problemId, tagId);
        return Result.ok("移除成功");
    }
    
    @PostMapping("/submissions/{submissionId}:setStatus")
    @Operation(summary = "修改提交状态", description = "管理员手动修改提交状态（用于 stub 阶段模拟判题结果）")
    public Result<Void> updateSubmissionStatus(
            @PathVariable Long submissionId,
            @Valid @RequestBody SubmissionStatusUpdateDTO dto) {
        submissionService.updateSubmissionStatus(submissionId, dto);
        return Result.ok("状态更新成功");
    }
    
    @PostMapping("/schools/{schoolId}/certify")
    @Operation(summary = "认证学校", description = "认证指定学校")
    public Result<Void> certifySchool(@PathVariable Long schoolId) {
        adminService.certifySchool(schoolId);
        return Result.ok("认证成功");
    }
    
    @DeleteMapping("/schools/{schoolId}/certify")
    @Operation(summary = "取消学校认证", description = "取消指定学校的认证")
    public Result<Void> uncertifySchool(@PathVariable Long schoolId) {
        adminService.uncertifySchool(schoolId);
        return Result.ok("取消认证成功");
    }
    
    // 数据统计
    @GetMapping("/statistics/overview")
    @Operation(summary = "获取概览统计", description = "获取系统概览统计数据")
    public Result<StatisticsVO> getOverviewStatistics() {
        StatisticsVO vo = adminService.getOverviewStatistics();
        return Result.ok(vo);
    }
    
    @GetMapping("/statistics/users")
    @Operation(summary = "获取用户统计", description = "获取用户相关统计数据")
    public Result<StatisticsVO> getUserStatistics() {
        StatisticsVO vo = adminService.getUserStatistics();
        return Result.ok(vo);
    }
    
    @GetMapping("/statistics/schools")
    @Operation(summary = "获取学校统计", description = "获取学校相关统计数据")
    public Result<StatisticsVO> getSchoolStatistics() {
        StatisticsVO vo = adminService.getSchoolStatistics();
        return Result.ok(vo);
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
        
        throw BusinessException.unauthorized("无法获取用户ID");
    }
}



