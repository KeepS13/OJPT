package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermissionCreateDTO {
    @NotBlank(message = "资源标识不能为空")
    private String resource;
    
    @NotBlank(message = "操作动作不能为空")
    private String action;
    
    private String conditionJson;
    private String description;
}




