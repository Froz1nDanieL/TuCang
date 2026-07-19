package com.mushan.tucangbackend.model.color;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 图片颜色离线分析结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColorAnalysisResult implements Serializable {

    private String dominantColor;

    private List<ColorPaletteItem> palette;

    private List<String> colorTags;

    private PictureColorScores colorScores;

    private Integer algorithmVersion;

    private static final long serialVersionUID = 1L;
}
