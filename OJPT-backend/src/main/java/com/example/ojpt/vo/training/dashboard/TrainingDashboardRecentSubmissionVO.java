package com.example.ojpt.vo.training.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDashboardRecentSubmissionVO {

    private Long submissionId;
    private Long problemId;
    private Integer problemNo;
    private String problemTitle;
    private String language;
    private String status;
    private Integer timeMs;
    private Integer memoryKb;
    private LocalDateTime createdAt;
}
