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

/**
 * 题目实体，对应表 problem。
 */
@Data
@Accessors(chain = true)
@TableName("problem")
public class Problem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 难度：EASY/MEDIUM/HARD
     */
    private String difficulty;

    /**
     * 题面 Markdown 内容
     */
    private String statementMd;

    /**
     * 时间限制（毫秒）
     */
    private Integer timeLimitMs;

    /**
     * 内存限制（KB）
     */
    private Integer memoryLimitKb;

    /**
     * 状态：DRAFT/PUBLISHED/ARCHIVED
     */
    private String status;

    /**
     * 提交总次数
     */
    private Long submitCount;

    /**
     * 通过次数
     */
    private Long acceptedCount;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}

