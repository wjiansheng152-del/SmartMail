/**
 * 与后端统一的响应与异常类型定义
 * 对应 common 模块 Result、ErrorResponse
 */

/** 统一成功响应：data 为业务数据 */
export interface Result<T> {
  data: T
}

/** 业务异常响应体 */
export interface ErrorResponse {
  errorCode: string
  errorInfo: string
}

/** 分页结果（与后端 IPage 对齐） */
export interface IPage<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 注册请求（用户名 1~64 字符，密码至少 6 位） */
export interface RegisterRequest {
  username: string
  password: string
}

/** 刷新 Token 请求 */
export interface RefreshRequest {
  refreshToken: string
}

/** Token 响应（含当前用户信息，登录/刷新后写入 store 以区分用户与租户） */
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  accessExpiresIn: number
  refreshExpiresIn: number
  userId?: number
  username?: string
  tenantId?: string
}
