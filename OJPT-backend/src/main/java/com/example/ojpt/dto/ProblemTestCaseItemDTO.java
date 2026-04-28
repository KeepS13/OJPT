package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProblemTestCaseItemDTO {

    private Long id;

    @NotBlank(message = "测试用例类型不能为空")
    private String caseType;

    @NotNull(message = "排序不能为空")
    private Integer sortOrder;

    @NotBlank(message = "输入不能为空")
    private String inputText;

    @NotBlank(message = "期望输出不能为空")
    private String expectedOutput;

    private String explanation;
}
