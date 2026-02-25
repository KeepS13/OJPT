package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.StudentService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.ClassMemberVO;
import com.example.ojpt.vo.ClassVO;
import com.example.ojpt.vo.UserDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "学生接口", description = "学生相关操作接口")
public class StudentController {
    
    private final StudentService studentService;
    private final UserService userService;
    
    /**
     * 获取个人完整信息（含扩展信息）
     */
    @GetMapping("/profile")
    @Operation(summary = "获取个人资料", description = "获取当前学生的完整个人信息")
    public Result<UserDetailVO> getProfile() {
        Long userId = getCurrentUserId();
        UserDetailVO vo = userService.getCurrentUserDetail(userId);
        return Result.ok(vo);
    }
    
    /**
     * 获取我加入的班级列表
     */
    @GetMapping("/classes")
    @Operation(summary = "获取我的班级列表", description = "获取当前学生加入的所有班级列表")
    public Result<PageResult<ClassVO>> getMyClasses(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        PageResult<ClassVO> classes = studentService.getMyClasses(userId, page, size);
        return Result.ok(classes);
    }
    
    /**
     * 获取班级详情
     */
    @GetMapping("/classes/{classId}")
    @Operation(summary = "获取班级详情", description = "获取指定班级的详细信息")
    public Result<ClassVO> getClassDetail(@PathVariable Long classId) {
        Long userId = getCurrentUserId();
        ClassVO vo = studentService.getClassDetail(userId, classId);
        return Result.ok(vo);
    }
    
    /**
     * 申请加入班级
     */
    @PostMapping("/classes/{classId}/apply")
    @Operation(summary = "申请加入班级", description = "申请加入指定班级，等待教师审核")
    public Result<Void> applyToClass(@PathVariable Long classId) {
        Long userId = getCurrentUserId();
        studentService.applyToClass(userId, classId);
        return Result.ok("申请成功，等待审核");
    }
    
    /**
     * 退出班级
     */
    @DeleteMapping("/classes/{classId}/quit")
    @Operation(summary = "退出班级", description = "退出指定班级")
    public Result<Void> quitClass(@PathVariable Long classId) {
        Long userId = getCurrentUserId();
        studentService.quitClass(userId, classId);
        return Result.ok("退出成功");
    }
    
    /**
     * 查看班级成员列表
     */
    @GetMapping("/classes/{classId}/members")
    @Operation(summary = "获取班级成员", description = "获取指定班级的所有成员列表")
    public Result<PageResult<ClassMemberVO>> getClassMembers(@PathVariable Long classId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        PageResult<ClassMemberVO> members = studentService.getClassMembers(userId, classId, page, size);
        return Result.ok(members);
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




