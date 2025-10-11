package com.mushan.tucangbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建文本生成图像请求
 */
@Data
public class CreateTextToImageRequest implements Serializable {

    /**
     * 正向提示词，用来描述生成图像中期望包含的元素和视觉特点
     */
    private String prompt;

    /**
     * 反向提示词，用来描述不希望在画面中看到的内容
     */
    private String negativePrompt;

    /**
     * 输出图像的分辨率，格式为宽*高
     */
    private String size;

    /**
     * 生成图片的数量，取值范围为1~4张
     */
    private Integer n;

    /**
     * 是否开启prompt智能改写
     */
    private Boolean promptExtend;

    /**
     * 是否添加水印标识
     */
    private Boolean watermark;

    /**
     * 随机数种子
     */
    private Integer seed;

    private static final long serialVersionUID = 1L;
}