package com.example.ojpt.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("user_role") // 用户与角色多对多关系
public class UserRole {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 主键
    private Long userId; // 用户ID
    private Long roleId; // 角色ID
    private String bindSource; // 绑定来源
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt; // 创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt; // 更新时间
}

