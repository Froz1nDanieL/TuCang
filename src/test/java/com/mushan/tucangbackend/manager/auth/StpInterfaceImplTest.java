package com.mushan.tucangbackend.manager.auth;

import com.mushan.tucangbackend.config.RequestWrapper;
import com.mushan.tucangbackend.manager.auth.model.SpaceUserPermissionConstant;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StpInterfaceImplTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldParseJsonWithCharsetAndUsePictureIdAsAuthoritativeResource() {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/picture/delete");
        rawRequest.setContextPath("/api");
        rawRequest.setServletPath("/picture/delete");
        rawRequest.setContentType("application/json;charset=UTF-8");
        rawRequest.setContent(("{\"id\":123,\"spaceId\":999,\"spaceUserId\":888,"
                + "\"spaceUser\":{\"spaceRole\":\"admin\"}}")
                .getBytes(StandardCharsets.UTF_8));
        RequestWrapper request = new RequestWrapper(rawRequest);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        StpInterfaceImpl stpInterface = new StpInterfaceImpl();
        SpaceUserAuthContext context = ReflectionTestUtils.invokeMethod(
                stpInterface, "getAuthContextByRequest");

        assertEquals(123L, context.getPictureId());
        assertNull(context.getSpaceId());
        assertNull(context.getSpaceUserId());
    }

    @Test
    void shouldReadPictureIdFromPathVariable() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/picture/like/123");
        request.setContextPath("/api");
        request.setServletPath("/picture/like/123");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                Collections.singletonMap("pictureId", "123"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        StpInterfaceImpl stpInterface = new StpInterfaceImpl();
        SpaceUserAuthContext context = ReflectionTestUtils.invokeMethod(
                stpInterface, "getAuthContextByRequest");

        assertEquals(123L, context.getPictureId());
        assertNull(context.getSpaceId());
        assertNull(context.getSpaceUserId());
    }

    @Test
    void emptyContextShouldNotGrantMutationOrMemberManagementPermissions() {
        StpInterfaceImpl stpInterface = new StpInterfaceImpl();
        UserService userService = mock(UserService.class);
        ReflectionTestUtils.setField(stpInterface, "userService", userService);
        User ordinaryUser = new User();
        ordinaryUser.setId(2L);
        when(userService.isAdmin(ordinaryUser)).thenReturn(false);
        List<String> adminPermissions = Arrays.asList(
                SpaceUserPermissionConstant.SPACE_USER_MANAGE,
                SpaceUserPermissionConstant.PICTURE_VIEW,
                SpaceUserPermissionConstant.PICTURE_UPLOAD,
                SpaceUserPermissionConstant.PICTURE_EDIT,
                SpaceUserPermissionConstant.PICTURE_DELETE
        );

        List<String> permissions = stpInterface.getPermissionsForEmptyContext(
                ordinaryUser, adminPermissions);

        assertEquals(Collections.singletonList(SpaceUserPermissionConstant.PICTURE_UPLOAD), permissions);
    }
}
