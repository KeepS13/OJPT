package com.example.ojpt.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("user_profile") // 用户扩展信息
public class UserProfile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 主键
    private Long userId; // 用户ID
    private Integer gender; // 性别：0未知/1男/2女
    private LocalDate birthday; // 生日
    private String address; // 现住址
    private String website; // 个人网站（博客或作品集等）
    private String github; // GitHub 用户名或链接
    private String company; // 所在公司
    private String position; // 职位
    private String skills; // 技能（逗号分隔或 JSON）
    private String studentNo; // 学号/工号
    private Long schoolId; // 学校ID
    private String bio; // 简介
    private String tags; // 标签
    private Integer identityStatus; // 实名/资质状态
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt; // 创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt; // 更新时间
}

