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
@TableName("school") // 学校表
public class School {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 主键
    private String name; // 学校名称
    private String contact; // 联系人/电话
    private Integer status; // 状态
    private LocalDateTime certifiedAt; // 认证时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt; // 创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt; // 更新时间
}

