package com.smartmail.common.exception;

/**
 * 统一错误码枚举。
 * <p>
 * 与 HTTP 状态码及接口规范中的 errorCode 对应，供 BizException 与 GlobalExceptionHandler 使用，
 * 保证所有接口返回的错误体格式一致（errorCode + errorInfo）。
 * </p>
 */
public enum ErrorCode {
    /** 未授权，如未携带或无效的 JWT */
    UNAUTHORIZED("401", "未授权"),
    /** 无权限访问当前资源 */
    FORBIDDEN("403", "无权限"),
    /** 请求的资源不存在 */
    NOT_FOUND("404", "资源不存在"),
    /** 请求参数不合法 */
    BAD_REQUEST("400", "请求参数错误"),
    /** 资源冲突，如唯一键重复 */
    CONFLICT("409", "资源冲突"),
    /** 业务校验未通过 */
    UNPROCESSABLE("422", "校验失败"),
    /** 服务器内部未预期错误 */
    INTERNAL_ERROR("500", "服务器内部错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
