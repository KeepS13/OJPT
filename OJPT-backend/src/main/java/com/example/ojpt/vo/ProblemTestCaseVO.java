package com.example.ojpt.vo;

import lombok.Data;

@Data
public class ProblemTestCaseVO {

    private Long id;

    private String caseType;

    private Integer sortOrder;

    private String inputText;

    private String expectedOutput;

    private String explanation;
}
