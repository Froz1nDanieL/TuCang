package com.mushan.tucangbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 图片
 * @TableName picture
 */
@TableName(value ="picture")
@Data
public class Picture {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private String tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 来源：UNKNOWN/LOCAL_UPLOAD/URL_UPLOAD/AI_TEXT/AI_OUTPAINT
     */
    private String sourceType;

    /**
     * 关联 AI 外部任务 ID
     */
    private String aiTaskId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 点赞数
     */
    private Integer likeCount = 0;

    /**
     * 收藏数
     */
    private Integer favoriteCount = 0;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * Lab 调色板（JSON 数组）
     */
    private String colorPalette;

    /**
     * 十种标准色标签（JSON 数组）
     */
    private String colorTags;

    /**
     * 十种标准色离线分数（JSON 对象）
     */
    private String colorScores;

    /**
     * 颜色分析算法版本
     */
    private Integer colorAlgoVersion;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}
