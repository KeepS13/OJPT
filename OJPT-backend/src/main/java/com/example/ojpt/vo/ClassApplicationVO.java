package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassApplicationVO {
    private Long id;
    private Long classId;
    private String className;
    private Long userId;
    private String username;
    private String email;
    private String avatar;
    private String studentNo;
    private String joinType;
    private String joinStatus;
    private LocalDateTime joinAt;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewAt;
    private String reviewComment;
}




