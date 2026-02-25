package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassCreateDTO {
    @NotNull(message = "院系ID不能为空")
    private Long departmentId;
    
    @NotBlank(message = "班级名称不能为空")
    private String name;
    
    private String year;
    private Long teacherId;
    private String merk;
}




