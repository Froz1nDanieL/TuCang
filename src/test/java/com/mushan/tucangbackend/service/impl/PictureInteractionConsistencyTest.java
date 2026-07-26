package com.mushan.tucangbackend.service.impl;

import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.mapper.PictureAlbumMapper;
import com.mushan.tucangbackend.mapper.PictureMapper;
import com.mushan.tucangbackend.mapper.UserPictureInteractionMapper;
import com.mushan.tucangbackend.model.dto.picture.PictureFavoriteRequest;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.PictureAlbum;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.service.PictureAlbumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PictureInteractionConsistencyTest {

    private PictureServiceImpl pictureService;
    private PictureMapper pictureMapper;
    private PictureAlbumMapper pictureAlbumMapper;
    private UserPictureInteractionMapper interactionMapper;
    private PictureAlbumService pictureAlbumService;
    private User loginUser;
    private Picture picture;
    private PictureAlbum album;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        pictureService = spy(new PictureServiceImpl());
        pictureMapper = mock(PictureMapper.class);
        pictureAlbumMapper = mock(PictureAlbumMapper.class);
        interactionMapper = mock(UserPictureInteractionMapper.class);
        pictureAlbumService = mock(PictureAlbumService.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(1L);

        ReflectionTestUtils.setField(pictureService, "baseMapper", pictureMapper);
        ReflectionTestUtils.setField(pictureService, "pictureAlbumMapper", pictureAlbumMapper);
        ReflectionTestUtils.setField(pictureService, "userPictureInteractionMapper", interactionMapper);
        ReflectionTestUtils.setField(pictureService, "pictureAlbumService", pictureAlbumService);
        ReflectionTestUtils.setField(pictureService, "redisTemplate", redisTemplate);

        loginUser = new User();
        loginUser.setId(10L);

        picture = new Picture();
        picture.setId(20L);
        picture.setUserId(30L);
        picture.setSpaceId(null);
        doReturn(picture).when(pictureService).getById(20L);

        album = new PictureAlbum();
        album.setId(40L);
        album.setUserId(loginUser.getId());
        when(pictureAlbumService.getById(40L)).thenReturn(album);
    }

    @Test
    void duplicateConcurrentLikeDoesNotIncreaseCountAgain() {
        when(interactionMapper.deleteLike(10L, 20L)).thenReturn(0);
        when(interactionMapper.insertLikeIgnore(anyLong(), eq(10L), eq(20L))).thenReturn(0);

        assertTrue(pictureService.likePicture(20L, loginUser));

        verify(pictureMapper, never()).adjustLikeCount(eq(20L), eq(1));
    }

    @Test
    void removingLikeDecreasesCountByActuallyDeletedRows() {
        when(interactionMapper.deleteLike(10L, 20L)).thenReturn(1);
        when(pictureMapper.adjustLikeCount(20L, -1)).thenReturn(1);

        assertFalse(pictureService.likePicture(20L, loginUser));

        verify(pictureMapper).adjustLikeCount(20L, -1);
        verify(interactionMapper, never()).insertLikeIgnore(anyLong(), anyLong(), anyLong());
    }

    @Test
    void duplicateConcurrentFavoriteDoesNotIncreaseCountersAgain() {
        PictureFavoriteRequest request = favoriteRequest();
        when(interactionMapper.deleteFavorite(10L, 20L, 40L)).thenReturn(0);
        when(interactionMapper.insertFavoriteIgnore(anyLong(), eq(10L), eq(20L), eq(40L)))
                .thenReturn(0);

        assertTrue(pictureService.favoritePicture(request, loginUser));

        verify(pictureMapper, never()).adjustFavoriteCount(eq(20L), eq(1));
        verify(pictureAlbumMapper, never()).adjustPictureCount(eq(40L), eq(1));
    }

    @Test
    void addingFavoriteUpdatesBothCountersAtomically() {
        PictureFavoriteRequest request = favoriteRequest();
        when(interactionMapper.deleteFavorite(10L, 20L, 40L)).thenReturn(0);
        when(interactionMapper.insertFavoriteIgnore(anyLong(), eq(10L), eq(20L), eq(40L)))
                .thenReturn(1);
        when(pictureMapper.adjustFavoriteCount(20L, 1)).thenReturn(1);
        when(pictureAlbumMapper.adjustPictureCount(40L, 1)).thenReturn(1);

        assertTrue(pictureService.favoritePicture(request, loginUser));

        verify(pictureMapper).adjustFavoriteCount(20L, 1);
        verify(pictureAlbumMapper).adjustPictureCount(40L, 1);
    }

    @Test
    void removingMissingFavoriteDoesNotDecreaseCounters() {
        PictureFavoriteRequest request = favoriteRequest();
        when(interactionMapper.deleteFavorite(10L, 20L, 40L)).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> pictureService.removePictureFromAlbum(request, loginUser));

        verify(pictureMapper, never()).adjustFavoriteCount(eq(20L), eq(-1));
        verify(pictureAlbumMapper, never()).adjustPictureCount(eq(40L), eq(-1));
    }

    @Test
    void allInteractionMutationsDeclareTransactions() throws NoSuchMethodException {
        assertNotNull(PictureServiceImpl.class
                .getMethod("likePicture", Long.class, User.class)
                .getAnnotation(Transactional.class));
        assertNotNull(PictureServiceImpl.class
                .getMethod("favoritePicture", PictureFavoriteRequest.class, User.class)
                .getAnnotation(Transactional.class));
        assertNotNull(PictureServiceImpl.class
                .getMethod("addPictureToAlbum", PictureFavoriteRequest.class, User.class)
                .getAnnotation(Transactional.class));
        assertNotNull(PictureServiceImpl.class
                .getMethod("removePictureFromAlbum", PictureFavoriteRequest.class, User.class)
                .getAnnotation(Transactional.class));
    }

    private PictureFavoriteRequest favoriteRequest() {
        PictureFavoriteRequest request = new PictureFavoriteRequest();
        request.setPictureId(20L);
        request.setAlbumId(40L);
        return request;
    }
}
