package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProblemCodeDraftSaveDTO {

    @NotBlank(message = "语言不能为空")
    @Size(max = 32, message = "语言长度不能超过32个字符")
    private String language;

    @NotNull(message = "代码不能为空")
    private String sourceCode;
}
