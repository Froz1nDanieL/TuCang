package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

@Data
public class AdminAiTaskStatsVO {
    private long totalCount;
    private long runningCount;
    private long succeededCount;
    private long failedCount;
    private double successRate;
    private long p50DurationMs;
    private long p95DurationMs;
}
