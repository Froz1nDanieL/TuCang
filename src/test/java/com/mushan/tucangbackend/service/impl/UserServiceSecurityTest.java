package com.mushan.tucangbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mushan.tucangbackend.constant.UserConstant;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.model.dto.user.UserQueryRequest;
import com.mushan.tucangbackend.model.entity.User;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceSecurityTest {

    @Test
    void disabledUserLosesExistingSessionImmediately() {
        User cached = new User();
        cached.setId(1L);
        User disabled = new User();
        disabled.setId(1L);
        disabled.setUserStatus(1);
        HttpSession session = mock(HttpSession.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(UserConstant.USER_LOGIN_STATE)).thenReturn(cached);
        UserServiceImpl service = spy(new UserServiceImpl());
        doReturn(disabled).when(service).getById(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getLoginUser(request)
        );

        assertEquals(ErrorCode.FORBIDDEN_ERROR.getCode(), exception.getCode());
        verify(session).removeAttribute(UserConstant.USER_LOGIN_STATE);
    }

    @Test
    void userSortFieldUsesFixedAllowlist() {
        UserServiceImpl service = new UserServiceImpl();
        UserQueryRequest malicious = new UserQueryRequest();
        malicious.setSortField("createTime desc; drop table user");
        malicious.setSortOrder("ascend");

        QueryWrapper<User> blocked = service.getQueryWrapper(malicious);
        assertFalse(blocked.getSqlSegment().toLowerCase().contains("drop table"));

        UserQueryRequest allowed = new UserQueryRequest();
        allowed.setSortField("createTime");
        allowed.setSortOrder("ascend");
        QueryWrapper<User> accepted = service.getQueryWrapper(allowed);
        assertTrue(accepted.getSqlSegment().contains("createTime"));
    }
}
