package com.mushan.tucangbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName ai_gen_history
 */
@TableName(value ="ai_gen_history")
@Data
public class AiGenHistory {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 生成图片URL
     */
    private String imageUrl;

    /**
     * 图片尺寸
     */
    private String imageSize;

    /**
     * 状态: 0-进行中, 1-成功, 2-失败
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;
}