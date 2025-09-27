package com.mushan.tucangbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片收藏夹视图
 */
@Data
public class PictureAlbumVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 是否公开（0-私有 1-公开）
     */
    private Integer isPublic;

    /**
     * 图片数量
     */
    private Integer pictureCount;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 是否已收藏该图片
     */
    private Boolean isPictureFavorited;

    private static final long serialVersionUID = 1L;
}
