package com.example.ojpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmissionCreateResultVO {
    private Long submissionId;
    private String status;
    private String message;
}
