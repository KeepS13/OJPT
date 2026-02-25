package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherVO {
    private Long teacherId;
    private String username;
    private String email;
    private String avatar;
    private String role; // 在班级中的角色：班主任/助教等
    private LocalDateTime createdAt;
}




