package com.example.ojpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前登录用户的完整详情（包含 user 与 user_profile 扩展信息）。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailVO {

    // ====== User 基础信息 ======
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private String roleType;
    private Integer status;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ====== UserProfile 扩展信息 ======
    private Integer gender;        // 性别：0未知/1男/2女
    private LocalDate birthday;    // 生日
    private String address;        // 现住址
    private String website;        // 个人网站（博客或作品集等）
    private String github;         // GitHub 用户名或链接
    private String company;        // 所在公司
    private String position;       // 职位
    private String skills;         // 技能（逗号分隔或 JSON）
    private String studentNo;      // 学号/工号
    private Long schoolId;         // 学校ID
    private String bio;            // 简介
    private String tags;           // 标签
    private Integer identityStatus; // 实名/资质状态
}


