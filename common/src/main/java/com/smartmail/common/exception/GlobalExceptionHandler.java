package com.smartmail.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 统一异常处理器，将各类异常转换为规范的 ErrorResponse 并设置对应 HTTP 状态码。
 * <p>
 * 各业务服务需通过 @ComponentScan 包含 com.smartmail.common，本类才会被加载。
 * 处理顺序：BadCredentialsException -> BizException -> 其他 Exception。
 * </p>
 */
@RestControllerAdvice(basePackages = "com.smartmail")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 登录失败（用户名或密码错误）时返回 401 及统一错误体 */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .errorCode(ErrorCode.UNAUTHORIZED.getCode())
                        .errorInfo("用户名或密码错误")
                        .build());
    }

    /** 业务异常：按 errorCode 映射 HTTP 状态码并返回 errorCode、errorInfo */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBizException(BizException ex, HttpServletRequest request) {
        HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .errorCode(ex.getErrorCode())
                        .errorInfo(ex.getErrorInfo())
                        .build());
    }

    /** 未捕获的异常统一返回 500，避免泄露内部信息；异常详情写入服务端日志便于排查 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("未捕获异常 path={} {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .errorCode(ErrorCode.INTERNAL_ERROR.getCode())
                        .errorInfo(ErrorCode.INTERNAL_ERROR.getMessage())
                        .build());
    }

    /** 将业务错误码映射为 HTTP 状态码 */
    private static HttpStatus mapErrorCodeToStatus(String code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (code) {
            case "401" -> HttpStatus.UNAUTHORIZED;
            case "403" -> HttpStatus.FORBIDDEN;
            case "404" -> HttpStatus.NOT_FOUND;
            case "400" -> HttpStatus.BAD_REQUEST;
            case "409" -> HttpStatus.CONFLICT;
            case "422" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
