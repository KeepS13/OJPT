package com.example.ojpt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CodeRunDTO {

    @NotBlank(message = "运行语言不能为空")
    private String language;

    @NotBlank(message = "运行代码不能为空")
    private String sourceCode;

    private Integer timeLimitMs;

    private Integer memoryLimitKb;

    @Valid
    @NotEmpty(message = "至少需要一个运行用例")
    @Size(max = 8, message = "单次最多运行 8 个用例")
    private List<CodeRunCaseDTO> cases;
}
