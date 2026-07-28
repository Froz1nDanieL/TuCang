package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AdminReviewStatsVO {
    private long pendingCount;
    private long oldestWaitSeconds;
    private long processedCount;
    private long rejectedCount;
    private long averageDurationMs;
    private Map<String, Long> rejectionReasons = new LinkedHashMap<>();
}
