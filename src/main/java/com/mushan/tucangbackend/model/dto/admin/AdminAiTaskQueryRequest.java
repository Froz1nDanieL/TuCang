package com.mushan.tucangbackend.model.dto.admin;

import com.mushan.tucangbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminAiTaskQueryRequest extends PageRequest {
    private String taskStatus;
    private Integer taskType;
    private String modelName;
    private Long userId;
}
