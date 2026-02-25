package com.example.ojpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前登录用户信息（不含 token），用于 accessToken 自动登录场景。
 */
@Data
@AllArgsConstructor
public class CurrentUserVO {

    private Long userId;
    private String username;
    private String email;
    private String avatar;
    private String roleType;
    private Integer status;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


