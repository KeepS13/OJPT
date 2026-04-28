package com.example.ojpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CodeRunCaseResultVO {
    private Integer caseIndex;
    private String status;
    private String inputText;
    private String expectedOutput;
    private String actualOutput;
    private String errorOutput;
    private Long timeMs;
    private String message;
}
