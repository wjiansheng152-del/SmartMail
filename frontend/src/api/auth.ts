/**
 * IAM 认证接口：登录、刷新 Token
 */
import request from '@/utils/request'
import type { LoginRequest, RefreshRequest, TokenResponse } from '@/types/api'

/** 登录 */
export function login(data: LoginRequest) {
  return request.post<TokenResponse>('/iam/auth/login', data)
}

/** 刷新 Token */
export function refreshToken(data: RefreshRequest) {
  return request.post<TokenResponse>('/iam/auth/refresh', data)
}
