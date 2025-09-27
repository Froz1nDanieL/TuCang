package com.mushan.tucangbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建收藏夹请求
 */
@Data
public class PictureAlbumAddRequest implements Serializable {

    /**
     * 收藏夹名称
     */
    private String name;

    /**
     * 收藏夹描述
     */
    private String description;

    /**
     * 是否公开 (0-私有, 1-公开)
     */
    private Integer isPublic;

    private static final long serialVersionUID = 1L;
}
