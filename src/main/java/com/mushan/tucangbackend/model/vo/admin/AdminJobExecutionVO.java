package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.util.Date;

@Data
public class AdminJobExecutionVO {
    private Long id;
    private String jobName;
    private String status;
    private String scopeType;
    private Long totalCount;
    private Long processedCount;
    private Long successCount;
    private Long failureCount;
    private String errorMessage;
    private Date startedTime;
    private Date completedTime;
    private Date createTime;
}
