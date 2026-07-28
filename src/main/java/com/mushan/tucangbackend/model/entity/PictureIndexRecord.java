package com.mushan.tucangbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("picture_index_record")
public class PictureIndexRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long pictureId;
    private String recordType;
    private String batchId;
    private String syncType;
    private String operation;
    private String mismatchTypes;
    private Integer success;
    private String errorMessage;
    private Long durationMs;
    private Integer resolved;
    private Date resolvedTime;
    private Long operatorId;
    private Date createTime;
}
