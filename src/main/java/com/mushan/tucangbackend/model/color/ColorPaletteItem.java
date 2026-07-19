package com.mushan.tucangbackend.model.color;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * 调色板中的一个感知颜色。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColorPaletteItem implements Serializable {

    @Field(type = FieldType.Keyword)
    private String hex;

    @Field(type = FieldType.Float)
    private double l;

    @Field(type = FieldType.Float)
    private double a;

    @Field(type = FieldType.Float)
    private double b;

    /**
     * 该颜色在有效采样像素中的占比，范围为 0 到 1。
     */
    @Field(type = FieldType.Float)
    private double weight;

    private static final long serialVersionUID = 1L;
}
