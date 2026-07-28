package com.mushan.tucangbackend.controller.admin;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.model.dto.admin.AdminUserUpdateRequest;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.UserRoleEnum;
import com.mushan.tucangbackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserControllerTest {

    private AdminUserController controller;
    private UserService userService;
    private HttpServletRequest servletRequest;

    @BeforeEach
    void setUp() {
        controller = new AdminUserController();
        userService = mock(UserService.class);
        servletRequest = mock(HttpServletRequest.class);
        ReflectionTestUtils.setField(controller, "userService", userService);
    }

    @Test
    void cannotDisableCurrentAdministrator() {
        User current = enabledAdmin(1L);
        when(userService.getLoginUser(servletRequest)).thenReturn(current);
        when(userService.getById(1L)).thenReturn(current);
        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setId(1L);
        request.setUserStatus(1);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> controller.update(request, servletRequest)
        );

        assertEquals(ErrorCode.FORBIDDEN_ERROR.getCode(), exception.getCode());
        verify(userService, never()).updateById(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void cannotDowngradeLastEnabledAdministrator() {
        User current = enabledAdmin(1L);
        User target = enabledAdmin(2L);
        when(userService.getLoginUser(servletRequest)).thenReturn(current);
        when(userService.getById(2L)).thenReturn(target);
        LambdaQueryChainWrapper<User> chain =
                mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        when(userService.lambdaQuery()).thenReturn(chain);
        when(chain.count()).thenReturn(1L);
        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setId(2L);
        request.setUserRole(UserRoleEnum.REVIEWER.getValue());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> controller.update(request, servletRequest)
        );

        assertEquals(ErrorCode.CONFLICT_ERROR.getCode(), exception.getCode());
        verify(userService, never()).updateById(org.mockito.ArgumentMatchers.any(User.class));
    }

    private User enabledAdmin(Long id) {
        User user = new User();
        user.setId(id);
        user.setUserRole(UserRoleEnum.ADMIN.getValue());
        user.setUserStatus(0);
        return user;
    }
}
