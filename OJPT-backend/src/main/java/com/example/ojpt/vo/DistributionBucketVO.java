package com.example.ojpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DistributionBucketVO {
    private String label;
    private Integer min;
    private Integer max;
    private Integer count;
}
