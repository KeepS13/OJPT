package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionVO {

    private Long id;

    private Long problemId;

    private String language;

    private String status;

    private Integer timeMs;

    private Integer memoryKb;

    private LocalDateTime createdAt;
}

