package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class AdminAiTaskVO {
    private Long id;
    private Long userId;
    private String userName;
    private String prompt;
    private String promptSummary;
    private String taskId;
    private Integer taskType;
    private Long sourcePictureId;
    private String modelName;
    private String requestId;
    private String taskStatus;
    private String requestParams;
    private List<String> resultUrls = new ArrayList<>();
    private Date createTime;
    private Date completedTime;
    private Long durationMs;
    private Integer resultCount;
    private String errorCode;
    private String errorMessage;
    private String retryFromTaskId;
}
