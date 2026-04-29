package com.example.ojpt.vo.training.dashboard;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserTrainingDashboardVO {

    private Long totalSubmissions;
    private Long acceptedSubmissions;
    private Long solvedProblemCount;
    private Double acceptanceRate;
    private List<TrainingDashboardRecentSubmissionVO> recentSubmissions;
    private Map<String, Long> statusDistribution;
    private Map<String, Long> difficultyDistribution;
}
