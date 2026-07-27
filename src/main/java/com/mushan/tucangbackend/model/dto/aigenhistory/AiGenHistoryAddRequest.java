package com.mushan.tucangbackend.model.dto.aigenhistory;

import lombok.Data;

import java.io.Serializable;

/**
 * AI生成历史添加请求
 *
 * @author mushan
 */
@Data
public class AiGenHistoryAddRequest implements Serializable {

    /**
     * 用户提示词
     */
    private String prompt;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * AI 任务类型：0-文生图，1-扩图
     */
    private Integer taskType;

    /**
     * 源图片 ID，仅扩图任务使用
     */
    private Long sourcePictureId;

    /**
     * 状态: 0-进行中, 1-成功, 2-失败
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
