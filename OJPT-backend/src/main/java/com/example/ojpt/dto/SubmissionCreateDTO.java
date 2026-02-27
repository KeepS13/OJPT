package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmissionCreateDTO {

    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    @NotBlank(message = "语言不能为空")
    private String language;

    @NotBlank(message = "代码不能为空")
    private String sourceCode;
}

