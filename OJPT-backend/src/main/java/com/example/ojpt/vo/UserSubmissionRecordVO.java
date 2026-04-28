package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserSubmissionRecordVO {
    private Long submissionId;
    private Long problemId;
    private Integer problemNo;
    private String problemTitle;
    private String language;
    private String status;
    private String sourceCode;
    private Integer timeMs;
    private Integer memoryKb;
    private String compileMessage;
    private String judgeMessage;
    private LocalDateTime createdAt;
}
