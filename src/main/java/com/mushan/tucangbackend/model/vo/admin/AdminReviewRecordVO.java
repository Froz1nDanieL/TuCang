package com.mushan.tucangbackend.model.vo.admin;

import lombok.Data;

import java.util.Date;

@Data
public class AdminReviewRecordVO {
    private Long id;
    private Long pictureId;
    private Integer fromStatus;
    private Integer toStatus;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerRole;
    private String reasonCode;
    private String reviewMessage;
    private Long durationMs;
    private Integer conflict;
    private Date createTime;
}
