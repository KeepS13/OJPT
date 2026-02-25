package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClassVO {
    private Long id;
    private Long departmentId;
    private String departmentName;
    private Long schoolId;
    private String schoolName;
    private String name;
    private String year;
    private Long teacherId;
    private String teacherName;
    private String merk;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 学员视角：加入状态
    private String joinStatus;
    private String joinType;
    private LocalDateTime joinAt;
}




