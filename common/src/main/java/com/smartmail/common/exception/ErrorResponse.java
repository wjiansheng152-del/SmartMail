package com.smartmail.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一异常响应体，符合接口规范 errorCode / errorInfo。
 * <p>
 * 所有接口在发生错误时均返回该结构，便于前端统一解析与展示。
 * 由 GlobalExceptionHandler 构造并写入响应。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** 错误码，如 401、404、500 */
    private String errorCode;
    /** 错误描述，面向用户或调用方 */
    private String errorInfo;
}
