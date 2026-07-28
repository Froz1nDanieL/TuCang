package com.mushan.tucangbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("admin_job_execution")
public class AdminJobExecution {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String jobName;
    private String triggerType;
    private String status;
    private String scopeType;
    private Long totalCount;
    private Long processedCount;
    private Long successCount;
    private Long failureCount;
    private String errorMessage;
    private Long operatorId;
    private String idempotencyKey;
    private Date startedTime;
    private Date completedTime;
    private Date createTime;
}
