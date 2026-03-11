/**
 * IAM 认证接口：登录、注册、刷新 Token
 */
import request from '@/utils/request'
import type { LoginRequest, RegisterRequest, RefreshRequest, TokenResponse } from '@/types/api'

/** 登录 */
export function login(data: LoginRequest) {
  return request.post<TokenResponse>('/iam/auth/login', data)
}

/** 注册（注册即创建新租户 + 该租户下唯一账号，无需 Token） */
export function register(data: RegisterRequest) {
  return request.post<unknown>('/iam/auth/register', data)
}

/** 刷新 Token */
export function refreshToken(data: RefreshRequest) {
  return request.post<TokenResponse>('/iam/auth/refresh', data)
}
