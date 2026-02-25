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
@TableName("class") // 班级/小组
public class Clazz {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 主键
    private Long departmentId; // 院系ID
    private String name; // 班级名称
    private String year; // 届/年份
    private Long teacherId; // 班主任/负责人
    private String merk; // 班级/小组类型/简介
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt; // 创建时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt; // 更新时间
}

