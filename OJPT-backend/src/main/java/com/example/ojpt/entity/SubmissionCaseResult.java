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
@TableName("submission_case_result")
public class SubmissionCaseResult {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long submissionId;

    private String caseType;

    private Integer caseIndex;

    private String inputText;

    private String expectedOutput;

    private String actualOutput;

    private String errorOutput;

    private String status;

    private Integer timeMs;

    private String message;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
