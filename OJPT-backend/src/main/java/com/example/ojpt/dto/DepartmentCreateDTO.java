package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentCreateDTO {
    @NotBlank(message = "院系名称不能为空")
    private String name;
}




