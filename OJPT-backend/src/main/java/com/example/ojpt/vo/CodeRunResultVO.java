package com.example.ojpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CodeRunResultVO {
    private String status;
    private List<CodeRunCaseResultVO> caseResults;
}
