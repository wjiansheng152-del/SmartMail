/**
 * 用户模块：登录、刷新 Token、登出、accessToken/refreshToken/userId/username/tenantId
 */
import request from '@/utils/request'
import type { LoginRequest, TokenResponse } from '@/types/api'

export interface UserState {
  accessToken: string
  refreshToken: string
  userId: number | null
  username: string
  tenantId: string
}

const state: UserState = {
  accessToken: '',
  refreshToken: '',
  userId: null,
  username: '',
  tenantId: 'default',
}

const STORAGE_KEYS = {
  ACCESS_TOKEN: 'smartmail_access_token',
  REFRESH_TOKEN: 'smartmail_refresh_token',
  USER_ID: 'smartmail_user_id',
  USERNAME: 'smartmail_username',
  TENANT_ID: 'smartmail_tenant_id',
}

function loadFromStorage() {
  state.accessToken = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN) ?? ''
  state.refreshToken = localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN) ?? ''
  const uid = localStorage.getItem(STORAGE_KEYS.USER_ID)
  state.userId = uid ? Number(uid) : null
  state.username = localStorage.getItem(STORAGE_KEYS.USERNAME) ?? ''
  state.tenantId = localStorage.getItem(STORAGE_KEYS.TENANT_ID) ?? 'default'
}

function saveToStorage() {
  if (state.accessToken) localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, state.accessToken)
  if (state.refreshToken) localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, state.refreshToken)
  if (state.userId != null) localStorage.setItem(STORAGE_KEYS.USER_ID, String(state.userId))
  if (state.username) localStorage.setItem(STORAGE_KEYS.USERNAME, state.username)
  if (state.tenantId) localStorage.setItem(STORAGE_KEYS.TENANT_ID, state.tenantId)
}

function clearStorage() {
  localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN)
  localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN)
  localStorage.removeItem(STORAGE_KEYS.USER_ID)
  localStorage.removeItem(STORAGE_KEYS.USERNAME)
  localStorage.removeItem(STORAGE_KEYS.TENANT_ID)
}

// 初始化时从 localStorage 恢复
loadFromStorage()

export const user = {
  namespaced: true,
  state,
  getters: {
    accessToken: (s: UserState) => s.accessToken,
    refreshToken: (s: UserState) => s.refreshToken,
    userId: (s: UserState) => s.userId,
    username: (s: UserState) => s.username,
    tenantId: (s: UserState) => s.tenantId,
    isLoggedIn: (s: UserState) => !!s.accessToken,
  },
  mutations: {
    SET_TOKENS(
      s: UserState,
      payload: { accessToken: string; refreshToken: string }
    ) {
      s.accessToken = payload.accessToken
      s.refreshToken = payload.refreshToken
      saveToStorage()
    },
    SET_USER(
      s: UserState,
      payload: { userId: number; username: string; tenantId?: string }
    ) {
      s.userId = payload.userId
      s.username = payload.username
      if (payload.tenantId !== undefined) s.tenantId = payload.tenantId
      saveToStorage()
    },
    LOGOUT(s: UserState) {
      s.accessToken = ''
      s.refreshToken = ''
      s.userId = null
      s.username = ''
      s.tenantId = 'default'
      clearStorage()
    },
  },
  actions: {
    async login(
      { commit }: { commit: (type: string, payload?: unknown) => void },
      payload: LoginRequest
    ): Promise<TokenResponse> {
      const { data } = await request.post<TokenResponse>('/iam/auth/login', payload)
      commit('SET_TOKENS', {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      })
      // 网关会在 JWT 中解析出 userId、username、tenantId，这里先占位，可选：登录接口若返回用户信息则写入
      commit('SET_USER', {
        userId: 0,
        username: payload.username,
        tenantId: 'default',
      })
      return data
    },
    async refreshToken({
      state,
      commit,
    }: {
      state: UserState
      commit: (type: string, payload?: unknown) => void
    }): Promise<void> {
      const { data } = await request.post<TokenResponse>('/iam/auth/refresh', {
        refreshToken: state.refreshToken,
      })
      commit('SET_TOKENS', {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
      })
    },
    logout({ commit }: { commit: (type: string) => void }) {
      commit('LOGOUT')
    },
  },
}
