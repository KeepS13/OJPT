package com.example.ojpt.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SubmissionCreateResultVO {
    private Long submissionId;
    private String status;
    private String message;
    private Integer timeMs;
    private Integer rank;
    private Integer totalCaseCount;
    private SubmissionRankStatsVO rankStats;
    private List<CodeRunCaseResultVO> caseResults;

    public SubmissionCreateResultVO(Long submissionId, String status, String message) {
        this.submissionId = submissionId;
        this.status = status;
        this.message = message;
    }

    public SubmissionCreateResultVO(
            Long submissionId,
            String status,
            String message,
            Integer timeMs,
            Integer rank,
            List<CodeRunCaseResultVO> caseResults) {
        this.submissionId = submissionId;
        this.status = status;
        this.message = message;
        this.timeMs = timeMs;
        this.rank = rank;
        this.totalCaseCount = caseResults == null ? 0 : caseResults.size();
        this.caseResults = caseResults;
    }

    public SubmissionCreateResultVO(
            Long submissionId,
            String status,
            String message,
            Integer timeMs,
            Integer rank,
            Integer totalCaseCount,
            SubmissionRankStatsVO rankStats,
            List<CodeRunCaseResultVO> caseResults) {
        this.submissionId = submissionId;
        this.status = status;
        this.message = message;
        this.timeMs = timeMs;
        this.rank = rank;
        this.totalCaseCount = totalCaseCount;
        this.rankStats = rankStats;
        this.caseResults = caseResults;
    }
}
