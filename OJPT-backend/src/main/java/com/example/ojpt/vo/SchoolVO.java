package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SchoolVO {
    private Long id;
    private String name;
    private String contact;
    private Integer status;
    private LocalDateTime certifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 统计信息
    private Long departmentCount;
    private Long classCount;
    private Long teacherCount;
    private Long studentCount;
}




