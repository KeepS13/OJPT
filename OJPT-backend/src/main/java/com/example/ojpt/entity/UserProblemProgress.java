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
 * 用户-题目进度实体，对应表 user_problem_progress。
 */
@Data
@Accessors(chain = true)
@TableName("user_problem_progress")
public class UserProblemProgress {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long problemId;

    /**
     * 做题状态：UNSOLVED/ATTEMPTED/SOLVED
     */
    private String status;

    private Long lastSubmissionId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

