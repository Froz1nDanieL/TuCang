package com.mushan.tucangbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName picture_album
 */
@TableName(value ="picture_album")
@Data
public class PictureAlbum {
    /**
     * 收藏夹ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 收藏夹名称
     */
    private String name;

    /**
     * 收藏夹描述
     */
    private String description;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 是否公开 (0-私有, 1-公开)
     */
    private Integer isPublic;

    /**
     * 图片数量
     */
    private Integer pictureCount;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private Date createTime;
}