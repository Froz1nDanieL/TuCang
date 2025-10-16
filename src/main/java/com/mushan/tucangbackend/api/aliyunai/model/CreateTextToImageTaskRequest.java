package com.mushan.tucangbackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreateTextToImageTaskRequest implements Serializable {

    /**
     * 模型名称，例如 "wanx2.0-t2i-turbo"
     */
    private String model = "wanx2.1-t2i-turbo";

    /**
     * 输入文本信息
     */
    private Input input;

    /**
     * 图像处理参数
     */
    private Parameters parameters;

    @Data
    public static class Input implements Serializable {
        /**
         * 必选，正向提示词
         */
        private String prompt;

        /**
         * 可选，反向提示词
         */
        @Alias("negative_prompt")
        private String negativePrompt;
    }

    @Data
    public static class Parameters implements Serializable {
        /**
         * 可选，输出图像的分辨率，格式为宽*高
         */
        private String size;

        /**
         * 可选，生成图片的数量，取值范围为1~4张，默认为1
         */
        private Integer n = 1;

        /**
         * 可选，是否开启prompt智能改写
         */
        @Alias("prompt_extend")
        private Boolean promptExtend;

        /**
         * 可选，是否添加水印标识
         */
        private Boolean watermark = false;

        /**
         * 可选，随机数种子
         */
        private Integer seed;
    }
}