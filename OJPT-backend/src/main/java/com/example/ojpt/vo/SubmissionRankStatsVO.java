package com.example.ojpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionRankStatsVO {
    private Integer acceptedCount;
    private List<DistributionBucketVO> timeBuckets;
}
