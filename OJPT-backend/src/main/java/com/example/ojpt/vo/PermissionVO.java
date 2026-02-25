package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PermissionVO {
    private Long id;
    private String resource;
    private String action;
    private String conditionJson;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}




