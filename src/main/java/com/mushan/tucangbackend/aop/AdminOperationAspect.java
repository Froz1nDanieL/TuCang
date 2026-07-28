package com.mushan.tucangbackend.aop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.mushan.tucangbackend.annotation.AdminOperation;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.model.entity.AdminOperationLog;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.service.AdminOperationLogService;
import com.mushan.tucangbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(20)
@Slf4j
public class AdminOperationAspect {

    private static final int MAX_PARAM_LENGTH = 2000;

    @Resource
    private UserService userService;

    @Resource
    private AdminOperationLogService operationLogService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(operation)")
    public Object record(ProceedingJoinPoint joinPoint, AdminOperation operation) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request = currentRequest();
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (RuntimeException ignored) {
            // Login failures can still be logged without an operator.
        }
        String requestParams = safeParams(joinPoint.getArgs());
        if (operation.idempotent() && loginUser != null) {
            assertNotDuplicate(request, loginUser, requestParams);
        }

        int resultCode = ErrorCode.SUCCESS.getCode();
        String errorMessage = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            resultCode = throwable instanceof BusinessException
                    ? ((BusinessException) throwable).getCode()
                    : ErrorCode.SYSTEM_ERROR.getCode();
            errorMessage = truncate(throwable.getMessage(), 512);
            throw throwable;
        } finally {
            if (loginUser == null) {
                try {
                    loginUser = userService.getLoginUser(request);
                } catch (RuntimeException ignored) {
                    // Failed logins remain anonymous.
                }
            }
            AdminOperationLog operationLog = new AdminOperationLog();
            if (loginUser != null) {
                operationLog.setOperatorId(loginUser.getId());
                operationLog.setOperatorName(loginUser.getUserName());
                operationLog.setOperatorRole(loginUser.getUserRole());
            }
            operationLog.setModule(operation.module());
            operationLog.setAction(operation.action());
            operationLog.setTargetType(operation.targetType());
            operationLog.setTargetId(extractTargetId(joinPoint.getArgs()));
            operationLog.setRequestMethod(request.getMethod());
            operationLog.setRequestPath(request.getRequestURI());
            operationLog.setRequestParams(requestParams);
            operationLog.setResultCode(resultCode);
            operationLog.setSuccess(resultCode == ErrorCode.SUCCESS.getCode() ? 1 : 0);
            operationLog.setErrorMessage(errorMessage);
            operationLog.setIp(resolveClientIp(request));
            operationLog.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
            operationLog.setDurationMs(System.currentTimeMillis() - start);
            operationLog.setCreateTime(new Date());
            try {
                operationLogService.save(operationLog);
            } catch (RuntimeException logException) {
                log.error("Failed to persist admin operation log", logException);
            }
        }
    }

    private void assertNotDuplicate(HttpServletRequest request, User user, String params) {
        String clientKey = request.getHeader("X-Idempotency-Key");
        String fingerprint = StrUtil.isNotBlank(clientKey)
                ? clientKey
                : DigestUtil.sha256Hex(request.getRequestURI() + ":" + params);
        String key = "tucang:admin:idempotency:" + user.getId() + ":" + fingerprint;
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException(ErrorCode.CONFLICT_ERROR, "请勿重复提交");
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private String safeParams(Object[] args) {
        List<Object> serializableArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null
                    || arg instanceof ServletRequest
                    || arg instanceof ServletResponse
                    || arg instanceof MultipartFile) {
                continue;
            }
            serializableArgs.add(arg);
        }
        String json;
        try {
            json = JSONUtil.toJsonStr(serializableArgs);
        } catch (RuntimeException ignored) {
            json = "[]";
        }
        json = json.replaceAll(
                "(?i)(\\\"[^\\\"]*(?:password|token|secret|cookie|authorization|apiKey|secretKey)[^\\\"]*\\\"\\s*:\\s*)\\\"[^\\\"]*\\\"",
                "$1\"***\""
        );
        return truncate(json, MAX_PARAM_LENGTH);
    }

    private String extractTargetId(Object[] args) {
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof Number || arg instanceof String) {
                return truncate(String.valueOf(arg), 128);
            }
            try {
                Method method = arg.getClass().getMethod("getId");
                Object id = method.invoke(arg);
                if (id != null) {
                    return truncate(String.valueOf(id), 128);
                }
            } catch (Exception ignored) {
                // Request object has no id.
            }
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwarded)) {
            return truncate(forwarded.split(",")[0].trim(), 64);
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
