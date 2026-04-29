import request from './request'
import type { LoginSuccessPayload, CurrentUser } from '@/types/auth'

export interface LoginPayload {
  account: string
  password: string
}

export interface RegisterPayload {
  account: string
  password: string
  nickname: string
  gender: 1 | 2
  birthday?: string
}

export type LoginResponse = LoginSuccessPayload

export interface PasswordResetRequestPayload {
  account: string
}

export interface RefreshPayload {
  refreshToken: string
}

export function login(payload: LoginPayload) {
  return request.post<LoginResponse>('/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return request.post<LoginResponse>('/auth/register', payload)
}

export function requestPasswordReset(payload: PasswordResetRequestPayload) {
  return request.post<void>('/auth/password-reset-requests', payload)
}

export function refreshToken(payload: RefreshPayload) {
  return request.post<LoginResponse>('/auth/refresh', payload)
}

export interface LogoutResponse {
  message: string
}

export function logout() {
  return request.post<LogoutResponse>('/auth/logout')
}

export function getCurrentUser() {
  return request.get<CurrentUser>('/auth/me')
}
