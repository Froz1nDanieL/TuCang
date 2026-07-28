package com.mushan.tucangbackend.model.vo.admin;

import com.mushan.tucangbackend.model.entity.AdminOperationLog;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class AdminOperationLogVO implements Serializable {

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
    private Integer resultCode;
    private Integer success;
    private String errorMessage;
    private String ip;
    private Long durationMs;
    private Date createTime;

    public static AdminOperationLogVO from(AdminOperationLog log) {
        AdminOperationLogVO vo = new AdminOperationLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
