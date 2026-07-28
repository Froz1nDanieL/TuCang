package com.mushan.tucangbackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<BaseResponse<?>> notLoginException(
            NotLoginException exception, HttpServletRequest request) {
        log.error("NotLoginException", exception);
        return response(ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, exception.getMessage()),
                HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<BaseResponse<?>> notPermissionExceptionHandler(
            NotPermissionException exception, HttpServletRequest request) {
        log.error("NotPermissionException", exception);
        return response(ResultUtils.error(ErrorCode.NO_AUTH_ERROR, exception.getMessage()),
                HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> businessExceptionHandler(
            BusinessException exception, HttpServletRequest request) {
        log.error("BusinessException", exception);
        return response(ResultUtils.error(exception.getCode(), exception.getMessage()),
                statusFor(exception.getCode()), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> validationExceptionHandler(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldError() == null
                ? ErrorCode.PARAMS_ERROR.getMessage()
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        return response(ResultUtils.error(ErrorCode.PARAMS_ERROR, message), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<?>> runtimeExceptionHandler(
            RuntimeException exception, HttpServletRequest request) {
        log.error("RuntimeException", exception);
        return response(ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误"),
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<BaseResponse<?>> response(
            BaseResponse<?> body, HttpStatus adminStatus, HttpServletRequest request) {
        if (request.getRequestURI() != null && request.getRequestURI().contains("/admin/")) {
            return ResponseEntity.status(adminStatus).body(body);
        }
        return ResponseEntity.ok(body);
    }

    private HttpStatus statusFor(int code) {
        if (code == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ErrorCode.NO_AUTH_ERROR.getCode() || code == ErrorCode.FORBIDDEN_ERROR.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == ErrorCode.NOT_FOUND_ERROR.getCode()) {
            return HttpStatus.NOT_FOUND;
        }
        if (code == ErrorCode.CONFLICT_ERROR.getCode()) {
            return HttpStatus.CONFLICT;
        }
        if (code == ErrorCode.PARAMS_ERROR.getCode()) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
