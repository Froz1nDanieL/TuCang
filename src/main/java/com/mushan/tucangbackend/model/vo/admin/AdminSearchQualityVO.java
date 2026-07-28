package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.util.Date;

@Data
public class AdminSearchQualityVO {
    private String elasticsearchStatus;
    private long openMismatchCount;
    private long recentSyncFailureCount;
    private Date lastCheckTime;
    private String lastCheckStatus;
}
