package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassMemberVO {
    private Long userId;
    private String username;
    private String email;
    private String avatar;
    private String studentNo;
    private LocalDateTime joinAt;
    private String joinType;
}




