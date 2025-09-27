package com.mushan.tucangbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户图片互动记录（点赞、收藏）
 * @TableName user_picture_interaction
 */
@TableName(value = "user_picture_interaction")
@Data
public class UserPictureInteraction implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 互动类型（0-点赞 1-收藏）
     */
    private Integer type;

    /**
     * 收藏夹ID
     */
    private Long albumId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
