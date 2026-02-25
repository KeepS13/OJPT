package com.example.ojpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 登录响应：token + 少量用户基础信息。
 */
@Data
@AllArgsConstructor
public class LoginResponseVO {

    // token 信息
    private String tokenType;
    private String accessToken;
    private long expiresIn;
    private String refreshToken;
    private long refreshExpiresIn;

    // 用户基础信息
    private Long userId;
    private String username;
    private String email;
    private String avatar;
    private String roleType;
    private List<String> roles; // 去除 ROLE_ 前缀后的角色列表
}


