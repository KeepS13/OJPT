package com.example.ojpt.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("user") // 用户表
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 主键
    private String username; // 登录名
    private String password; // 密码（BCrypt）
    private String email; // 邮箱
    private String phone; // 手机号
    private Integer status; // 0禁用/1启用/2待审核
    private LocalDateTime lastLoginAt; // 最近登录时间
    private String avatar; // 头像
    private String roleType; // 默认角色

    @TableLogic
    private Integer isDeleted; // 逻辑删除标记

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt; // 创建时间

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt; // 更新时间

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy; // 创建人

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy; // 更新人
}

