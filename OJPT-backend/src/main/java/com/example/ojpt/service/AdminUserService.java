package com.example.ojpt.service;

import com.example.ojpt.dto.AdminUserCreateDTO;
import com.example.ojpt.vo.AdminUserVO;

public interface AdminUserService {

    /**
     * 管理员创建用户，并绑定角色。
     *
     * @param dto 创建参数
     * @return 创建后的用户信息（脱敏）
     */
    AdminUserVO createUser(AdminUserCreateDTO dto);

    /**
     * 管理员拉黑用户（强制下线）。
     *
     * @param userId 用户ID
     * @param ttlSeconds 封号时长（秒）；若为 null 或 ≤0 则使用默认值
     */
    void blacklistUser(Long userId, Long ttlSeconds);

    /**
     * 管理员恢复用户（从黑名单移除）。
     *
     * @param userId 用户ID
     */
    void unblacklistUser(Long userId);
}

