package com.mushan.tucangbackend.model.vo.admin;

import cn.hutool.json.JSONUtil;
import com.mushan.tucangbackend.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
public class AdminPictureVO implements Serializable {

    private Long id;
    private String url;
    private String thumbnailUrl;
    private String name;
    private String introduction;
    private String category;
    private List<String> tags;
    private Long picSize;
    private Integer picWidth;
    private Integer picHeight;
    private String picFormat;
    private Long userId;
    private String userName;
    private String spaceName;
    private Long spaceId;
    private String sourceType;
    private String aiTaskId;
    private Integer reviewStatus;
    private String reviewMessage;
    private Long reviewerId;
    private Date reviewTime;
    private Date createTime;
    private Date updateTime;
    private Integer likeCount;
    private Integer favoriteCount;
    private Long waitSeconds;
    private Long recentRejectCount;

    public static AdminPictureVO from(Picture picture) {
        AdminPictureVO vo = new AdminPictureVO();
        BeanUtils.copyProperties(picture, vo);
        vo.setTags(picture.getTags() == null
                ? Collections.<String>emptyList()
                : JSONUtil.toList(picture.getTags(), String.class));
        return vo;
    }

    private static final long serialVersionUID = 1L;
}
