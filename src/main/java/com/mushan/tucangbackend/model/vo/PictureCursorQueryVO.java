package com.mushan.tucangbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片游标查询结果视图
 */
@Data
public class PictureCursorQueryVO implements Serializable {

    /**
     * 图片列表
     */
    private List<PictureVO> pictureList;

    /**
     * 下一个游标ID
     */
    private Long nextCursorId;

    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;

    private static final long serialVersionUID = 1L;
}