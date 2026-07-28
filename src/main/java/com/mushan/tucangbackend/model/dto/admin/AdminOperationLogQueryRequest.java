package com.mushan.tucangbackend.model.dto.admin;

import com.mushan.tucangbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminOperationLogQueryRequest extends PageRequest implements Serializable {

    private Long operatorId;
    private String module;
    private String action;
    private Integer success;
    private Date startTime;
    private Date endTime;

    private static final long serialVersionUID = 1L;
}
