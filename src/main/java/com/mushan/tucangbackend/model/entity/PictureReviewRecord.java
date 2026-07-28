package com.mushan.tucangbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("picture_review_record")
public class PictureReviewRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long pictureId;
    private Integer fromStatus;
    private Integer toStatus;
    private Long reviewerId;
    private String reviewerRole;
    private String reasonCode;
    private String reviewMessage;
    private Long durationMs;
    private Integer conflict;
    private Date createTime;
}
