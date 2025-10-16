package com.mushan.tucangbackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * AI生成历史视图对象
 *
 * @author mushan
 */
@Data
public class AiGenHistoryVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户提示词
     */
    private String prompt;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 生成图片URL列表
     */
    private List<String> imageUrlList;


    /**
     * 状态: 0-进行中, 1-成功, 2-失败
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}