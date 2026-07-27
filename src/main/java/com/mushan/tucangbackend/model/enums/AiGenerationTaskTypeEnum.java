package com.mushan.tucangbackend.model.enums;

import lombok.Getter;

/**
 * AI 图片任务类型。
 */
@Getter
public enum AiGenerationTaskTypeEnum {

    TEXT_TO_IMAGE(0),
    OUT_PAINTING(1);

    private final int value;

    AiGenerationTaskTypeEnum(int value) {
        this.value = value;
    }
}
