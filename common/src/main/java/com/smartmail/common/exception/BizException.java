package com.smartmail.common.exception;

import lombok.Getter;

/**
 * 业务异常，携带错误码与错误信息。
 * <p>
 * 在 Service 或 Controller 中抛出后，由 {@link GlobalExceptionHandler} 捕获并转换为
 * 符合接口规范的 JSON 响应（errorCode、errorInfo），并映射到对应的 HTTP 状态码。
 * </p>
 */
@Getter
public class BizException extends RuntimeException {

    /** 业务错误码，与 ErrorCode 或自定义字符串一致 */
    private final String errorCode;
    /** 返回给前端的错误描述 */
    private final String errorInfo;

    /** 使用枚举中的默认文案 */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.errorInfo = errorCode.getMessage();
    }

    /** 使用枚举错误码，自定义文案 */
    public BizException(ErrorCode errorCode, String errorInfo) {
        super(errorInfo);
        this.errorCode = errorCode.getCode();
        this.errorInfo = errorInfo;
    }

    /** 完全自定义错误码与文案 */
    public BizException(String errorCode, String errorInfo) {
        super(errorInfo);
        this.errorCode = errorCode;
        this.errorInfo = errorInfo;
    }
}
