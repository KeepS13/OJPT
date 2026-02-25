import request from './request'
import type { LoginSuccessPayload, CurrentUser } from '@/types/auth'

// 登录请求体：账号（邮箱或手机号）+ 密码
export interface LoginPayload {
  account: string
  password: string
}

// 登录 / 刷新 token 统一响应体（LoginResponseVO）
export type LoginResponse = LoginSuccessPayload

// 刷新 token 请求体
export interface RefreshPayload {
  refreshToken: string
}

// 登录
export function login(payload: LoginPayload) {
  return request.post<LoginResponse>('/auth/login', payload)
}

// 刷新 token
export function refreshToken(payload: RefreshPayload) {
  return request.post<LoginResponse>('/auth/refresh', payload)
}

// 登出
export interface LogoutResponse {
  message: string
}

export function logout() {
  return request.post<LogoutResponse>('/auth/logout')
}

// 使用 accessToken 获取当前登录用户信息（自动登录）
export function getCurrentUser() {
  return request.get<CurrentUser>('/auth/me')
}
