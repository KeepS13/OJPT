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
 * 提交记录实体，对应表 submission。
 */
@Data
@Accessors(chain = true)
@TableName("submission")
public class Submission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long problemId;

    private String language;

    private String sourceCode;

    /**
     * 判题状态：QUEUED/RUNNING/AC/WA/TLE/MLE/RE/CE/SYSTEM_ERROR
     */
    private String status;

    private Integer timeMs;

    private Integer memoryKb;

    private String compileMessage;

    private String judgeMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

