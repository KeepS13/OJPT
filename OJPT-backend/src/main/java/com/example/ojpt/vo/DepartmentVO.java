package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DepartmentVO {
    private Long id;
    private Long schoolId;
    private String schoolName;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}




