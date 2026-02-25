package com.example.ojpt.controller;

import com.example.ojpt.common.Result;
import com.example.ojpt.dto.AdminUserCreateDTO;
import com.example.ojpt.vo.AdminUserVO;
import com.example.ojpt.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "管理员用户接口", description = "管理员创建和管理用户相关接口")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 管理员创建用户。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "管理员创建用户", description = "管理员创建新用户账号")
    public Result<AdminUserVO> create(@Valid @RequestBody AdminUserCreateDTO dto) {
        AdminUserVO vo = adminUserService.createUser(dto);
        return Result.ok("创建成功", vo);
    }

    /**
     * 管理员封号（拉黑）用户，支持指定封号时长。
     * 将用户加入黑名单，在封号时长内该用户的所有 token 立即失效。
     *
     * @param userId 用户ID
     * @param durationSeconds 封号时长（秒）；为空或 <=0 时使用默认 30 天
     * @return 操作成功响应
     */
    @PostMapping("/{userId}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "封禁用户", description = "管理员封禁（拉黑）用户，支持指定封禁时长（秒）；为空或<=0使用默认30天")
    public Result<Map<String, Object>> blacklistUser(@PathVariable Long userId,
                                                     @RequestParam(value = "durationSeconds", required = false) Long durationSeconds) {
        adminUserService.blacklistUser(userId, durationSeconds);
        long applied = (durationSeconds == null || durationSeconds <= 0) ? 30L * 24 * 60 * 60 : durationSeconds;
        return Result.ok("用户已封禁", Map.of("durationSeconds", applied));
    }

    /**
     * 管理员恢复用户（从黑名单移除）。
     * 用户恢复后可以正常登录和使用系统。
     *
     * @param userId 用户ID
     * @return 操作成功响应
     */
    @DeleteMapping("/{userId}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "恢复用户", description = "管理员将用户从黑名单移除，恢复正常登录/使用")
    public Result<Void> unblacklistUser(@PathVariable Long userId) {
        adminUserService.unblacklistUser(userId);
        return Result.ok("用户已恢复");
    }
}

