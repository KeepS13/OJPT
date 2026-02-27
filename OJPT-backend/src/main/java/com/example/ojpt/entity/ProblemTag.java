package com.example.ojpt.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 题目与标签关联实体，对应表 problem_tag。
 */
@Data
@Accessors(chain = true)
@TableName("problem_tag")
public class ProblemTag {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long problemId;

    private Long tagId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

