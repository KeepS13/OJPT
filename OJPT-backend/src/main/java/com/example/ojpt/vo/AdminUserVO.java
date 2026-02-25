package com.example.ojpt.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员用户视图对象，脱敏输出。
 */
@Data
public class AdminUserVO {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private Integer status;
    private String roleType;
    private List<String> roleCodes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

