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
@TableName("role") // 角色表
public class Role {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 主键
    private String code; // 角色编码
    private String name; // 角色名称
    private String description; // 描述
    private Integer level; // 角色层级/优先级
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt; // 创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt; // 更新时间
}

