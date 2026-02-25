package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.dto.DepartmentCreateDTO;
import com.example.ojpt.dto.DepartmentUpdateDTO;
import com.example.ojpt.dto.SchoolUpdateDTO;
import com.example.ojpt.dto.UserUpdateDTO;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.SchoolService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.ClassMemberVO;
import com.example.ojpt.vo.ClassVO;
import com.example.ojpt.vo.DepartmentVO;
import com.example.ojpt.vo.SchoolVO;
import com.example.ojpt.vo.StatisticsVO;
import com.example.ojpt.vo.TeacherVO;
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
@RequestMapping("/api/school")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "学校接口", description = "学校管理员相关操作接口")
public class SchoolController {
    
    private final SchoolService schoolService;
    private final UserService userService;
    
    @GetMapping("/info")
    @Operation(summary = "获取学校信息", description = "获取当前学校的基本信息")
    public Result<SchoolVO> getSchoolInfo() {
        Long schoolUserId = getCurrentUserId();
        SchoolVO vo = schoolService.getSchoolInfo(schoolUserId);
        return Result.ok(vo);
    }
    
    @PutMapping("/info")
    @Operation(summary = "更新学校信息", description = "更新当前学校的基本信息")
    public Result<Void> updateSchoolInfo(@Valid @RequestBody SchoolUpdateDTO dto) {
        Long schoolUserId = getCurrentUserId();
        schoolService.updateSchoolInfo(schoolUserId, dto);
        return Result.ok("更新成功");
    }
    
    @GetMapping("/certification")
    @Operation(summary = "获取认证信息", description = "获取当前学校的认证状态信息")
    public Result<SchoolVO> getCertification() {
        Long schoolUserId = getCurrentUserId();
        SchoolVO vo = schoolService.getCertification(schoolUserId);
        return Result.ok(vo);
    }
    
    @GetMapping("/departments")
    public Result<PageResult<DepartmentVO>> getDepartments(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long schoolUserId = getCurrentUserId();
        PageResult<DepartmentVO> departments = schoolService.getDepartments(schoolUserId, page, size);
        return Result.ok(departments);
    }
    
    @PostMapping("/departments")
    public Result<DepartmentVO> createDepartment(@Valid @RequestBody DepartmentCreateDTO dto) {
        Long schoolUserId = getCurrentUserId();
        DepartmentVO vo = schoolService.createDepartment(schoolUserId, dto);
        return Result.ok("创建成功", vo);
    }
    
    @GetMapping("/departments/{departmentId}")
    public Result<DepartmentVO> getDepartment(@PathVariable Long departmentId) {
        Long schoolUserId = getCurrentUserId();
        DepartmentVO vo = schoolService.getDepartment(schoolUserId, departmentId);
        return Result.ok(vo);
    }
    
    @PutMapping("/departments/{departmentId}")
    public Result<Void> updateDepartment(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentUpdateDTO dto) {
        Long schoolUserId = getCurrentUserId();
        schoolService.updateDepartment(schoolUserId, departmentId, dto);
        return Result.ok("更新成功");
    }
    
    @DeleteMapping("/departments/{departmentId}")
    public Result<Void> deleteDepartment(@PathVariable Long departmentId) {
        Long schoolUserId = getCurrentUserId();
        schoolService.deleteDepartment(schoolUserId, departmentId);
        return Result.ok("删除成功");
    }
    
    @GetMapping("/classes")
    public Result<PageResult<ClassVO>> getClasses(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long schoolUserId = getCurrentUserId();
        PageResult<ClassVO> classes = schoolService.getClasses(schoolUserId, page, size);
        return Result.ok(classes);
    }
    
    @GetMapping("/departments/{departmentId}/classes")
    public Result<PageResult<ClassVO>> getDepartmentClasses(@PathVariable Long departmentId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long schoolUserId = getCurrentUserId();
        PageResult<ClassVO> classes = schoolService.getDepartmentClasses(schoolUserId, departmentId, page, size);
        return Result.ok(classes);
    }
    
    @GetMapping("/classes/{classId}")
    public Result<ClassVO> getClassDetail(@PathVariable Long classId) {
        Long schoolUserId = getCurrentUserId();
        ClassVO vo = schoolService.getClassDetail(schoolUserId, classId);
        return Result.ok(vo);
    }
    
    @PutMapping("/classes/{classId}")
    public Result<Void> updateClass(
            @PathVariable Long classId,
            @Valid @RequestBody com.example.ojpt.dto.ClassUpdateDTO dto) {
        Long schoolUserId = getCurrentUserId();
        schoolService.updateClass(schoolUserId, classId, dto);
        return Result.ok("更新成功");
    }
    
    @DeleteMapping("/classes/{classId}")
    public Result<Void> deleteClass(@PathVariable Long classId) {
        Long schoolUserId = getCurrentUserId();
        schoolService.deleteClass(schoolUserId, classId);
        return Result.ok("删除成功");
    }
    
    @GetMapping("/teachers")
    @Operation(summary = "获取教师列表", description = "获取当前学校的所有教师列表")
    public Result<PageResult<TeacherVO>> getTeachers(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long schoolUserId = getCurrentUserId();
        PageResult<TeacherVO> teachers = schoolService.getTeachers(schoolUserId, page, size);
        return Result.ok(teachers);
    }
    
    @PostMapping("/teachers")
    public Result<TeacherVO> addTeacher(@RequestBody Map<String, Long> request) {
        Long schoolUserId = getCurrentUserId();
        Long userId = request.get("userId");
        TeacherVO vo = schoolService.addTeacher(schoolUserId, userId);
        return Result.ok("创建成功", vo);
    }
    
    @GetMapping("/teachers/{teacherId}")
    public Result<TeacherVO> getTeacher(@PathVariable Long teacherId) {
        Long schoolUserId = getCurrentUserId();
        TeacherVO vo = schoolService.getTeacher(schoolUserId, teacherId);
        return Result.ok(vo);
    }
    
    @PutMapping("/teachers/{teacherId}")
    public Result<Void> updateTeacher(
            @PathVariable Long teacherId,
            @Valid @RequestBody UserUpdateDTO dto) {
        Long schoolUserId = getCurrentUserId();
        schoolService.updateTeacher(schoolUserId, teacherId, dto);
        return Result.ok("更新成功");
    }
    
    @DeleteMapping("/teachers/{teacherId}")
    public Result<Void> removeTeacher(@PathVariable Long teacherId) {
        Long schoolUserId = getCurrentUserId();
        schoolService.removeTeacher(schoolUserId, teacherId);
        return Result.ok("移除成功");
    }
    
    @GetMapping("/teachers/{teacherId}/classes")
    public Result<PageResult<ClassVO>> getTeacherClasses(@PathVariable Long teacherId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long schoolUserId = getCurrentUserId();
        PageResult<ClassVO> classes = schoolService.getTeacherClasses(schoolUserId, teacherId, page, size);
        return Result.ok(classes);
    }
    
    @GetMapping("/students")
    public Result<PageResult<UserDetailVO>> getStudents(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long schoolUserId = getCurrentUserId();
        PageResult<UserDetailVO> students = schoolService.getStudents(schoolUserId, page, size);
        return Result.ok(students);
    }
    
    @GetMapping("/departments/{departmentId}/students")
    public Result<PageResult<UserDetailVO>> getDepartmentStudents(@PathVariable Long departmentId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long schoolUserId = getCurrentUserId();
        PageResult<UserDetailVO> students = schoolService.getDepartmentStudents(schoolUserId, departmentId, page, size);
        return Result.ok(students);
    }
    
    @GetMapping("/classes/{classId}/students")
    public Result<PageResult<ClassMemberVO>> getClassStudents(@PathVariable Long classId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long schoolUserId = getCurrentUserId();
        PageResult<ClassMemberVO> students = schoolService.getClassStudents(schoolUserId, classId, page, size);
        return Result.ok(students);
    }
    
    @GetMapping("/students/{studentId}")
    public Result<UserDetailVO> getStudent(@PathVariable Long studentId) {
        Long schoolUserId = getCurrentUserId();
        UserDetailVO vo = schoolService.getStudent(schoolUserId, studentId);
        return Result.ok(vo);
    }
    
    @PutMapping("/students/{studentId}")
    public Result<Void> updateStudent(
            @PathVariable Long studentId,
            @Valid @RequestBody UserUpdateDTO dto) {
        Long schoolUserId = getCurrentUserId();
        schoolService.updateStudent(schoolUserId, studentId, dto);
        return Result.ok("更新成功");
    }
    
    @GetMapping("/statistics/overview")
    public Result<StatisticsVO> getOverviewStatistics() {
        Long schoolUserId = getCurrentUserId();
        StatisticsVO vo = schoolService.getOverviewStatistics(schoolUserId);
        return Result.ok(vo);
    }
    
    @GetMapping("/statistics/departments")
    public Result<List<Map<String, Object>>> getDepartmentStatistics() {
        Long schoolUserId = getCurrentUserId();
        List<Map<String, Object>> stats = schoolService.getDepartmentStatistics(schoolUserId);
        return Result.ok(stats);
    }
    
    @GetMapping("/statistics/classes")
    public Result<List<Map<String, Object>>> getClassStatistics() {
        Long schoolUserId = getCurrentUserId();
        List<Map<String, Object>> stats = schoolService.getClassStatistics(schoolUserId);
        return Result.ok(stats);
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




