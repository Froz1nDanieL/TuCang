package com.mushan.tucangbackend.aop;

import com.mushan.tucangbackend.annotation.AdminPermission;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.service.AdminPermissionService;
import com.mushan.tucangbackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@Order(10)
public class AdminPermissionInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private AdminPermissionService adminPermissionService;

    @Around("@annotation(adminPermission)")
    public Object check(ProceedingJoinPoint joinPoint, AdminPermission adminPermission) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        User loginUser = userService.getLoginUser(request);
        if (!adminPermissionService.hasPermission(loginUser, adminPermission.value())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }
}
