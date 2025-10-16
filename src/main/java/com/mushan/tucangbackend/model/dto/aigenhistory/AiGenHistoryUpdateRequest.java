package com.mushan.tucangbackend.model.dto.aigenhistory;


import lombok.Data;

import java.io.Serializable;

/**
 * AI生成历史更新请求(在轮询获取结果时使用)
 *
 * @author mushan
 */
@Data
public class AiGenHistoryUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户提示词
     */
    private String prompt;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 生成图片URL
     */
    private String imageUrl;


    /**
     * 状态: 0-进行中, 1-成功, 2-失败
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}