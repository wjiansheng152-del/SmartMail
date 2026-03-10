package com.smartmail.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一成功响应包装。
 * <p>
 * 正常接口返回时使用 Result.ok(data)，前端可统一解析 data 字段获取业务数据。
 * 失败时由 GlobalExceptionHandler 返回 ErrorResponse，不经过本类。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 业务数据，可为单条记录、列表或 null */
    private T data;

    /** 构造带数据的成功响应 */
    public static <T> Result<T> ok(T data) {
        return Result.<T>builder().data(data).build();
    }

    /** 构造无业务数据的成功响应（如删除、更新仅需 200） */
    public static <T> Result<T> ok() {
        return ok(null);
    }
}
