package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmissionCreateDTO {
    @NotBlank(message = "提交语言不能为空")
    private String language;

    @NotBlank(message = "提交代码不能为空")
    private String sourceCode;
}
