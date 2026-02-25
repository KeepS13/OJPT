package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.dto.ApplicationReviewDTO;
import com.example.ojpt.dto.ClassCreateDTO;
import com.example.ojpt.dto.ClassUpdateDTO;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.TeacherService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.ClassApplicationVO;
import com.example.ojpt.vo.ClassMemberVO;
import com.example.ojpt.vo.ClassVO;
import com.example.ojpt.vo.TeacherVO;
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

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "教师接口", description = "教师相关操作接口")
public class TeacherController {
    
    private final TeacherService teacherService;
    private final UserService userService;
    
    @GetMapping("/classes")
    @Operation(summary = "获取我的班级列表", description = "获取当前教师负责/参与的班级列表")
    public Result<PageResult<ClassVO>> getMyClasses(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long teacherId = getCurrentUserId();
        PageResult<ClassVO> classes = teacherService.getMyClasses(teacherId, page, size);
        return Result.ok(classes);
    }
    
    @PostMapping("/classes")
    @Operation(summary = "创建班级", description = "创建新的班级")
    public Result<ClassVO> createClass(@Valid @RequestBody ClassCreateDTO dto) {
        Long teacherId = getCurrentUserId();
        ClassVO vo = teacherService.createClass(teacherId, dto);
        return Result.ok("创建成功", vo);
    }
    
    @GetMapping("/classes/{classId}")
    @Operation(summary = "获取班级详情", description = "获取指定班级的详细信息")
    public Result<ClassVO> getClassDetail(@PathVariable Long classId) {
        Long teacherId = getCurrentUserId();
        ClassVO vo = teacherService.getClassDetail(teacherId, classId);
        return Result.ok(vo);
    }
    
    @PutMapping("/classes/{classId}")
    @Operation(summary = "更新班级信息", description = "更新指定班级的信息")
    public Result<Void> updateClass(
            @PathVariable Long classId,
            @Valid @RequestBody ClassUpdateDTO dto) {
        Long teacherId = getCurrentUserId();
        teacherService.updateClass(teacherId, classId, dto);
        return Result.ok("更新成功");
    }
    
    @DeleteMapping("/classes/{classId}")
    @Operation(summary = "删除班级", description = "删除指定班级")
    public Result<Void> deleteClass(@PathVariable Long classId) {
        Long teacherId = getCurrentUserId();
        teacherService.deleteClass(teacherId, classId);
        return Result.ok("删除成功");
    }
    
    @GetMapping("/classes/{classId}/students")
    @Operation(summary = "获取班级学生列表", description = "获取指定班级的学生成员列表")
    public Result<PageResult<ClassMemberVO>> getClassStudents(@PathVariable Long classId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long teacherId = getCurrentUserId();
        PageResult<ClassMemberVO> students = teacherService.getClassStudents(teacherId, classId, page, size);
        return Result.ok(students);
    }
    
    @GetMapping("/classes/{classId}/applications")
    @Operation(summary = "获取入班申请列表", description = "获取指定班级的入班申请列表")
    public Result<PageResult<ClassApplicationVO>> getApplications(@PathVariable Long classId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long teacherId = getCurrentUserId();
        PageResult<ClassApplicationVO> applications = teacherService.getApplications(teacherId, classId, page, size);
        return Result.ok(applications);
    }
    
    @PostMapping("/classes/{classId}/applications/{applicationId}/approve")
    @Operation(summary = "批准入班申请", description = "批准指定入班申请")
    public Result<Void> approveApplication(
            @PathVariable Long classId,
            @PathVariable Long applicationId,
            @RequestBody(required = false) ApplicationReviewDTO dto) {
        Long teacherId = getCurrentUserId();
        teacherService.approveApplication(teacherId, classId, applicationId, dto);
        return Result.ok("批准成功");
    }
    
    @PostMapping("/classes/{classId}/applications/{applicationId}/reject")
    @Operation(summary = "拒绝入班申请", description = "拒绝指定入班申请")
    public Result<Void> rejectApplication(
            @PathVariable Long classId,
            @PathVariable Long applicationId,
            @RequestBody(required = false) ApplicationReviewDTO dto) {
        Long teacherId = getCurrentUserId();
        teacherService.rejectApplication(teacherId, classId, applicationId, dto);
        return Result.ok("拒绝成功");
    }
    
    @PostMapping("/classes/{classId}/students/{studentId}/invite")
    @Operation(summary = "邀请学生加入班级", description = "邀请指定学生加入指定班级")
    public Result<Void> inviteStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        Long teacherId = getCurrentUserId();
        teacherService.inviteStudent(teacherId, classId, studentId);
        return Result.ok("邀请成功");
    }
    
    @DeleteMapping("/classes/{classId}/students/{studentId}")
    @Operation(summary = "移除班级学生", description = "将指定学生从指定班级移除")
    public Result<Void> removeStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        Long teacherId = getCurrentUserId();
        teacherService.removeStudent(teacherId, classId, studentId);
        return Result.ok("移除成功");
    }
    
    @GetMapping("/classes/{classId}/teachers")
    @Operation(summary = "获取班级教师列表", description = "获取指定班级的教师列表")
    public Result<PageResult<TeacherVO>> getClassTeachers(@PathVariable Long classId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long teacherId = getCurrentUserId();
        PageResult<TeacherVO> teachers = teacherService.getClassTeachers(teacherId, classId, page, size);
        return Result.ok(teachers);
    }
    
    @PostMapping("/classes/{classId}/teachers")
    @Operation(summary = "添加教师到班级", description = "将指定教师添加到指定班级")
    public Result<Void> addTeacherToClass(
            @PathVariable Long classId,
            @RequestParam Long teacherId,
            @RequestParam(required = false) String role) {
        Long currentTeacherId = getCurrentUserId();
        teacherService.addTeacherToClass(currentTeacherId, classId, teacherId, role);
        return Result.ok("添加成功");
    }
    
    @DeleteMapping("/classes/{classId}/teachers/{teacherId}")
    @Operation(summary = "从班级移除教师", description = "将指定教师从指定班级移除")
    public Result<Void> removeTeacherFromClass(
            @PathVariable Long classId,
            @PathVariable Long teacherId) {
        Long currentTeacherId = getCurrentUserId();
        teacherService.removeTeacherFromClass(currentTeacherId, classId, teacherId);
        return Result.ok("移除成功");
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




