package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class AdminIndexRecordVO {
    private Long id;
    private Long pictureId;
    private String recordType;
    private String batchId;
    private String syncType;
    private String operation;
    private List<String> mismatchTypes = new ArrayList<>();
    private Integer success;
    private String errorMessage;
    private Long durationMs;
    private Integer resolved;
    private Date resolvedTime;
    private Date createTime;
}
