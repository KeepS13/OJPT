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
@TableName("class_teacher") // 班级-教师关系
public class ClassTeacher {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 主键
    private Long classId; // 班级ID
    private Long teacherId; // 教师ID
    private String role; // 角色：班主任/助教等
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt; // 创建时间
}

