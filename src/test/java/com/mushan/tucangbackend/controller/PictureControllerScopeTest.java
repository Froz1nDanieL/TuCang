package com.mushan.tucangbackend.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.model.dto.picture.PictureQueryRequest;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.enums.PictureReviewStatusEnum;
import com.mushan.tucangbackend.model.vo.PictureVO;
import com.mushan.tucangbackend.service.PictureCacheService;
import com.mushan.tucangbackend.service.PictureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PictureControllerScopeTest {

    private PictureController pictureController;
    private PictureService pictureService;
    private ValueOperations<String, String> valueOperations;
    private PictureCacheService pictureCacheService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        pictureController = new PictureController();
        pictureService = mock(PictureService.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        pictureCacheService = mock(PictureCacheService.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(pictureCacheService.getVersion()).thenReturn("7");

        ReflectionTestUtils.setField(pictureController, "pictureService", pictureService);
        ReflectionTestUtils.setField(pictureController, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(pictureController, "pictureCacheService", pictureCacheService);
    }

    @Test
    void publicCachedEndpointRejectsSpaceQuery() {
        PictureQueryRequest request = new PictureQueryRequest();
        request.setSpaceId(123L);

        assertThrows(BusinessException.class, () ->
                pictureController.listPublicPictureVOByPageCache(
                        request, new MockHttpServletRequest()));

        verifyNoInteractions(pictureService, valueOperations);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicCachedEndpointForcesPublicReviewedScopeAndUsesNamespacedRedisKey() {
        PictureQueryRequest request = new PictureQueryRequest();
        Page<Picture> picturePage = new Page<>(1, 10, 0);
        Page<PictureVO> pictureVOPage = new Page<>(1, 10, 0);

        when(valueOperations.get(anyString())).thenReturn(null);
        when(pictureService.getQueryWrapper(request)).thenReturn(new QueryWrapper<>());
        when(pictureService.page(
                any(Page.class),
                any(Wrapper.class))).thenReturn(picturePage);
        when(pictureService.getPictureVOPage(any(Page.class), any())).thenReturn(pictureVOPage);

        pictureController.listPublicPictureVOByPageCache(
                request, new MockHttpServletRequest());

        assertTrue(request.isNullSpaceId());
        assertEquals(PictureReviewStatusEnum.PASS.getValue(), request.getReviewStatus());
        verify(valueOperations).get(org.mockito.ArgumentMatchers.startsWith(
                "tucang:listPictureVOByPage:7:"));
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.startsWith("tucang:listPictureVOByPage:7:"),
                anyString(),
                eq(5L),
                eq(java.util.concurrent.TimeUnit.MINUTES));
    }
}
