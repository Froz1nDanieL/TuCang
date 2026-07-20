package com.mushan.tucangbackend.service.impl;

import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.model.dto.picture.PictureEditRequest;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PictureServiceAuthorizationTest {

    private PictureServiceImpl pictureService;
    private User loginUser;
    private Picture otherUsersPublicPicture;

    @BeforeEach
    void setUp() {
        pictureService = spy(new PictureServiceImpl());
        UserService userService = mock(UserService.class);
        ReflectionTestUtils.setField(pictureService, "userService", userService);

        loginUser = new User();
        loginUser.setId(2L);
        when(userService.isAdmin(loginUser)).thenReturn(false);

        otherUsersPublicPicture = new Picture();
        otherUsersPublicPicture.setId(123L);
        otherUsersPublicPicture.setUserId(1L);
        otherUsersPublicPicture.setSpaceId(null);
        doReturn(otherUsersPublicPicture).when(pictureService).getById(123L);
    }

    @Test
    void ordinaryUserCannotDeleteOtherUsersPublicPicture() {
        assertThrows(BusinessException.class,
                () -> pictureService.deletePicture(123L, loginUser));

        verify(pictureService, never()).removeById(123L);
    }

    @Test
    void ordinaryUserCannotEditOtherUsersPublicPicture() {
        PictureEditRequest request = new PictureEditRequest();
        request.setId(123L);
        request.setName("unauthorized edit");

        assertThrows(BusinessException.class,
                () -> pictureService.editPicture(request, loginUser));

        verify(pictureService, never()).updateById(org.mockito.ArgumentMatchers.any(Picture.class));
    }
}
