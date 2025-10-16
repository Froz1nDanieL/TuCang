package com.mushan.tucangbackend.model.dto.aigenhistory;

import com.mushan.tucangbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * AI生成历史查询请求
 *
 * @author mushan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AiGenHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 用户提示词
     */
    private String prompt;

    /**
     * 生成图片URL
     */
    private String imageUrl;


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