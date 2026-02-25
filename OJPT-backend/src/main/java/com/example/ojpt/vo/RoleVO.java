package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer level;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 权限列表
    private List<PermissionVO> permissions;
    private Long permissionCount;
}




