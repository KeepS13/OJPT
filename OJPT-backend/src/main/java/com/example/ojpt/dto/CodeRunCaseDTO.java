package com.example.ojpt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeRunCaseDTO {

    @NotNull(message = "运行输入不能为 null")
    private String inputText;

    @NotNull(message = "期望输出不能为 null")
    private String expectedOutput;
}
