package com.example.ojpt.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 用户更新个人信息 DTO。
 * 注意：不允许修改 username、password、avatar（头像需通过单独接口上传）。
 */
@Data
public class UserUpdateDTO {

    /**
     * 邮箱（可选，若提供则需唯一）
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号（可选，若提供则需唯一）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    // ========== 用户扩展信息（UserProfile）字段 ==========

    /**
     * 性别：0未知/1男/2女
     */
    @Min(value = 0, message = "性别值无效")
    @Max(value = 2, message = "性别值无效")
    private Integer gender;

    /**
     * 生日
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthday;

    /**
     * 现住址
     */
    private String address;

    /**
     * 个人网站（博客或作品集等）
     */
    private String website;

    /**
     * GitHub 用户名或链接
     */
    private String github;

    /**
     * 所在公司
     */
    private String company;

    /**
     * 职位
     */
    private String position;

    /**
     * 技能（逗号分隔或 JSON）
     */
    private String skills;

    /**
     * 学号/工号
     */
    private String studentNo;

    /**
     * 学校ID
     */
    private Long schoolId;

    /**
     * 简介
     */
    private String bio;

    /**
     * 标签（逗号分隔或 JSON）
     */
    private String tags;
}

