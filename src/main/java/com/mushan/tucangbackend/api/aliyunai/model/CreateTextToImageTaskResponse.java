package com.mushan.tucangbackend.api.aliyunai.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateTextToImageTaskResponse implements Serializable {

    /**
     * 任务输出信息
     */
    private Output output;

    /**
     * 请求唯一标识
     */
    private String requestId;

    /**
     * 接口错误码
     * <p>接口成功请求不会返回该参数</p>
     */
    private String code;

    /**
     * 接口错误信息
     * <p>接口成功请求不会返回该参数</p>
     */
    private String message;

    @Data
    public static class Output {
        /**
         * 任务ID
         */
        private String taskId;

        /**
         * 任务状态
         * PENDING：任务排队中
         * RUNNING：任务处理中
         * SUCCEEDED：任务执行成功
         * FAILED：任务执行失败
         * CANCELED：任务已取消
         * UNKNOWN：任务不存在或状态未知
         */
        private String taskStatus;
    }
}