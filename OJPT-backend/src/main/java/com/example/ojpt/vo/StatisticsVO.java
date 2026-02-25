package com.example.ojpt.vo;

import lombok.Data;

import java.util.Map;

@Data
public class StatisticsVO {
    private Long totalCount;
    private Map<String, Long> statusCount;
    private Map<String, Long> roleCount;
    private Long recentCount;
}




