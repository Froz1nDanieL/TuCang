package com.mushan.tucangbackend.model.color;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * 图片与产品十种标准色的离线相似度，范围为 0 到 1。
 */
@Data
public class PictureColorScores implements Serializable {

    @Field(type = FieldType.Float)
    private double red;
    @Field(type = FieldType.Float)
    private double orange;
    @Field(type = FieldType.Float)
    private double yellow;
    @Field(type = FieldType.Float)
    private double green;
    @Field(type = FieldType.Float)
    private double cyan;
    @Field(type = FieldType.Float)
    private double blue;
    @Field(type = FieldType.Float)
    private double purple;
    @Field(type = FieldType.Float)
    private double pink;
    @Field(type = FieldType.Float)
    private double black;
    @Field(type = FieldType.Float)
    private double white;

    public void setScore(String colorKey, double score) {
        switch (colorKey) {
            case "red":
                red = score;
                break;
            case "orange":
                orange = score;
                break;
            case "yellow":
                yellow = score;
                break;
            case "green":
                green = score;
                break;
            case "cyan":
                cyan = score;
                break;
            case "blue":
                blue = score;
                break;
            case "purple":
                purple = score;
                break;
            case "pink":
                pink = score;
                break;
            case "black":
                black = score;
                break;
            case "white":
                white = score;
                break;
            default:
                throw new IllegalArgumentException("不支持的标准色: " + colorKey);
        }
    }

    public double getScore(String colorKey) {
        switch (colorKey) {
            case "red":
                return red;
            case "orange":
                return orange;
            case "yellow":
                return yellow;
            case "green":
                return green;
            case "cyan":
                return cyan;
            case "blue":
                return blue;
            case "purple":
                return purple;
            case "pink":
                return pink;
            case "black":
                return black;
            case "white":
                return white;
            default:
                throw new IllegalArgumentException("不支持的标准色: " + colorKey);
        }
    }

    private static final long serialVersionUID = 1L;
}
