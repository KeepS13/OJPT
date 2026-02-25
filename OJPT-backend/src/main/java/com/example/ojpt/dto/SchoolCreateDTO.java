package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SchoolCreateDTO {
    @NotBlank(message = "学校名称不能为空")
    private String name;
    
    private String contact;
    private Integer status;
}




