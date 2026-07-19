package com.mushan.tucangbackend.model.es;

import cn.hutool.json.JSONUtil;
import com.mushan.tucangbackend.model.color.ColorAnalysisResult;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.utils.ColorPaletteUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PictureEsDTOTest {

    @Test
    void shouldConvertColorJsonToSearchDocumentAndBack() {
        ColorAnalysisResult analysis = ColorPaletteUtils.fromAverageColor("#D94B4B");
        Picture picture = new Picture();
        picture.setId(1L);
        picture.setPicColor(analysis.getDominantColor());
        picture.setColorPalette(JSONUtil.toJsonStr(analysis.getPalette()));
        picture.setColorTags(JSONUtil.toJsonStr(analysis.getColorTags()));
        picture.setColorScores(JSONUtil.toJsonStr(analysis.getColorScores()));
        picture.setColorAlgoVersion(analysis.getAlgorithmVersion());

        PictureEsDTO dto = PictureEsDTO.objToDto(picture);

        assertFalse(dto.getColorPalette().isEmpty());
        assertFalse(dto.getColorTags().isEmpty());
        assertNotNull(dto.getColorScores());
        assertEquals(ColorPaletteUtils.ALGORITHM_VERSION, dto.getColorAlgoVersion());

        Picture converted = PictureEsDTO.dtoToObj(dto);
        assertEquals(picture.getColorTags(), converted.getColorTags());
        assertEquals(picture.getColorAlgoVersion(), converted.getColorAlgoVersion());
    }
}
