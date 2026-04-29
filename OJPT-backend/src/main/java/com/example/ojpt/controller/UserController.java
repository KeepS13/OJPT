package com.example.ojpt.controller;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.Result;
import com.example.ojpt.dto.EmailUpdateDTO;
import com.example.ojpt.dto.PasswordUpdateDTO;
import com.example.ojpt.dto.PhoneUpdateDTO;
import com.example.ojpt.dto.UserUpdateDTO;
import com.example.ojpt.dto.UsernameUpdateDTO;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.service.SubmissionService;
import com.example.ojpt.service.TrainingDashboardService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.CurrentUserVO;
import com.example.ojpt.vo.UserSubmissionRecordVO;
import com.example.ojpt.vo.UserDetailVO;
import com.example.ojpt.vo.training.dashboard.UserTrainingDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 与当前登录用户相关的接口（例如头像上传等）。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户接口", description = "当前登录用户相关操作接口")
public class UserController {

    private final UserService userService;
    private final SubmissionService submissionService;
    private final TrainingDashboardService trainingDashboardService;

    // 单个头像文件最大 1MB（前端已压缩为 320x320 webp，这里做安全兜底）
    private static final long MAX_AVATAR_SIZE_BYTES = 1 * 1024 * 1024;

    /**
     * 当前用户上传或删除头像。
     * - 上传头像：前端需以 multipart/form-data 方式提交，字段名为 file，文件为 320x320 的 webp。
     * - 删除头像：传入 file=null，将删除后端存储的头像文件并将数据库中的 avatar 字段设置为 null。
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传/删除头像", description = "上传或删除当前用户的头像，仅支持webp格式，最大1MB")
    public Result<Map<String, String>> uploadAvatar(@RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        // 获取当前登录用户 ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        // 如果 file 为 null 或为空（前端未传文件或传了空文件），表示删除头像
        if (file == null || file.isEmpty()) {
            userService.deleteAvatar(userId);
            return Result.ok("头像删除成功", Map.of("avatar", ""));
        }

        // 文件验证（此时 file 不为空）
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw BusinessException.badRequest("头像文件过大，必须小于 1MB");
        }

        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();
        boolean isWebp = (contentType != null && contentType.contains("image/webp"))
                || (originalName != null && originalName.toLowerCase().endsWith(".webp"));
        if (!isWebp) {
            throw BusinessException.badRequest("仅支持 webp 格式头像");
        }

        // 调用 Service 层处理头像上传（包含删除旧头像、保存新头像、更新数据库）
        String avatarUrl = userService.uploadAvatar(userId, file);

        return Result.ok("头像上传成功", Map.of("avatar", avatarUrl));
    }

    /**
     * 查询当前登录用户的个人详情。
     * 返回用户完整信息，包括角色列表。
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的基本信息")
    public Result<CurrentUserVO> getCurrentUserInfo() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        CurrentUserVO vo = userService.getCurrentUserInfo(userId);
        return Result.ok(vo);
    }

    /**
     * 查询当前登录用户的完整详情（包含扩展信息 user_profile）。
     */
    @GetMapping("/me/detail")
    @Operation(summary = "获取当前用户详情", description = "获取当前登录用户的完整详细信息")
    public Result<UserDetailVO> getCurrentUserDetail() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        UserDetailVO vo = userService.getCurrentUserDetail(userId);
        return Result.ok(vo);
    }

    @GetMapping("/me/submissions")
    @Operation(summary = "获取当前用户解题记录", description = "分页获取当前登录用户的历史代码提交与结果")
    public Result<PageResult<UserSubmissionRecordVO>> getCurrentUserSubmissions(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        return Result.ok(submissionService.getCurrentUserSubmissions(userId, page, size));
    }

    @GetMapping("/me/training-dashboard")
    @Operation(summary = "获取当前用户训练看板", description = "聚合返回当前用户的训练统计、最近提交和状态分布")
    public Result<UserTrainingDashboardVO> getCurrentUserTrainingDashboard() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("鏈櫥褰曟垨鐧诲綍鐘舵€佸凡澶辨晥");
        }

        return Result.ok(trainingDashboardService.getTrainingDashboard(userId));
    }

    /**
     * 更新当前登录用户的个人信息。
     * 注意：不允许修改 username、password、avatar（头像需通过单独接口上传）。
     */
    @PutMapping("/me")
    @Operation(summary = "更新个人信息", description = "更新当前登录用户的个人信息")
    public Result<Void> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        userService.updateProfile(userId, dto);
        return Result.ok("个人信息更新成功");
    }

    /**
     * 修改用户名。
     */
    @PutMapping("/me/username")
    @Operation(summary = "修改用户名", description = "修改当前登录用户的用户名")
    public Result<Void> updateUsername(@Valid @RequestBody UsernameUpdateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        userService.updateUsername(userId, dto);
        return Result.ok("用户名修改成功");
    }

    /**
     * 修改邮箱（无需验证码，直接修改）。
     * 注意：也可以复用现有的 PUT /api/users/me 接口，传入 email 字段即可。
     */
    @PutMapping("/me/email")
    @Operation(summary = "修改邮箱", description = "修改当前登录用户的邮箱")
    public Result<Void> updateEmail(@Valid @RequestBody EmailUpdateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        userService.updateEmail(userId, dto);
        return Result.ok("邮箱修改成功");
    }

    /**
     * 修改手机号（无需验证码，直接修改）。
     * 注意：也可以复用现有的 PUT /api/users/me 接口，传入 phone 字段即可。
     */
    @PutMapping("/me/phone")
    @Operation(summary = "修改手机号", description = "修改当前登录用户的手机号")
    public Result<Void> updatePhone(@Valid @RequestBody PhoneUpdateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        userService.updatePhone(userId, dto);
        return Result.ok("手机号修改成功");
    }

    /**
     * 修改密码。
     */
    @PutMapping("/me/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        userService.updatePassword(userId, dto);
        return Result.ok("密码修改成功");
    }

    /**
     * 注销账号（软删除）。
     */
    @DeleteMapping("/me")
    @Operation(summary = "注销账号", description = "注销当前登录用户的账号（软删除）")
    public Result<Void> deleteAccount() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录或登录状态已失效");
        }

        userService.deleteAccount(userId);
        return Result.ok("账号注销成功");
    }

    /**
     * 从 SecurityContext 获取当前登录用户的 ID。
     * 优先尝试从 LoginUserDetails 获取，其次尝试从 Long 类型的 principal（userId）获取，
     * 最后尝试从 String 类型的 principal（username，兼容旧 token）查询用户。
     *
     * @return 用户 ID，如果未登录则返回 null
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        // 优先尝试从 LoginUserDetails 获取
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUserDetails loginUserDetails) {
            return loginUserDetails.getUserId();
        }

        // 如果 principal 是 Long（userId），直接返回（JwtAuthenticationFilter 使用 userId 作为 principal）
        if (principal instanceof Long userId) {
            return userId;
        }

        // 兼容旧 token：如果 principal 是字符串（username），则通过 username 查询用户
        if (principal instanceof String username) {
            var user = userService.findByUsername(username);
            return user != null ? user.getId() : null;
        }

        return null;
    }
}
