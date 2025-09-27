package com.mushan.tucangbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 收藏图片请求
 */
@Data
public class PictureFavoriteRequest implements Serializable {

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 收藏夹ID
     */
    private Long albumId;

    private static final long serialVersionUID = 1L;
}
