package com.mushan.tucangbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("admin_operation_log")
public class AdminOperationLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String module;
    private String action;
    private String targetType;
    private String targetId;
    private String requestMethod;
    private String requestPath;
    private String requestParams;
    private Integer resultCode;
    private Integer success;
    private String errorMessage;
    private String ip;
    private String userAgent;
    private Long durationMs;
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
