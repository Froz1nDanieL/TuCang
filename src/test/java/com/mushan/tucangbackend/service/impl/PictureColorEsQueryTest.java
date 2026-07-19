package com.mushan.tucangbackend.service.impl;

import com.mushan.tucangbackend.model.dto.picture.PictureCursorQueryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PictureColorEsQueryTest {

    @Test
    void shouldBuildTermAndOfflineRankQueryWithSearchAfter() {
        PictureCursorQueryRequest request = new PictureCursorQueryRequest();
        request.setPageSize(16);
        request.setPicColor("#D94B4B");
        request.setCursorScore(1.25D);
        request.setCursorId(100L);

        NativeSearchQuery query = ReflectionTestUtils.invokeMethod(
                new PictureServiceImpl(),
                "buildEsQuery",
                request
        );

        String queryJson = query.getQuery().toString();
        assertTrue(queryJson.contains("colorTags"));
        assertTrue(queryJson.contains("colorScores.red"));
        assertFalse(queryJson.contains("script"));
        assertEquals(17, query.getPageable().getPageSize());
        assertEquals(Arrays.asList(1.25D, 100L), query.getSearchAfter());
        assertEquals(2, query.getElasticsearchSorts().size());
    }
}
