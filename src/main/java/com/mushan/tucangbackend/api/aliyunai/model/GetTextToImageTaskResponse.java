package com.mushan.tucangbackend.api.aliyunai.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GetTextToImageTaskResponse implements Serializable {

    /**
     * 请求唯一标识
     */
    private String requestId;

    /**
     * 任务输出信息
     */
    private Output output;

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

        /**
         * 提交时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String submitTime;

        /**
         * 调度时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String scheduledTime;

        /**
         * 结束时间
         * 格式：YYYY-MM-DD HH:mm:ss.SSS
         */
        private String endTime;

        /**
         * 输出图像结果列表
         */
        private List<Result> results;

        /**
         * 任务指标信息
         */
        private TaskMetrics taskMetrics;
    }

    /**
     * 表示单个图像生成结果
     */
    @Data
    public static class Result {
        /**
         * 原始提示词
         */
        private String origPrompt;

        /**
         * 实际使用的提示词
         */
        private String actualPrompt;

        /**
         * 图像URL
         */
        private String url;

        /**
         * 图像ID
         */
        private String imageId;

        /**
         * 随机数种子
         */
        private Integer seed;
    }

    /**
     * 表示任务的统计信息
     */
    @Data
    public static class TaskMetrics {

        /**
         * 总任务数
         */
        private Integer total;

        /**
         * 成功任务数
         */
        private Integer succeeded;

        /**
         * 失败任务数
         */
        private Integer failed;
    }
}