import { defineStore } from 'pinia'
import type { AuthUser, LoginSuccessPayload } from '@/types/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: '' as string | null,
    refreshToken: '' as string | null,
    user: null as AuthUser | null,
  }),
  actions: {
    setTokens(access: string, refresh: string) {
      this.accessToken = access
      this.refreshToken = refresh
    },
    setFromLogin(payload: LoginSuccessPayload) {
      this.accessToken = payload.accessToken
      this.refreshToken = payload.refreshToken
      // 将空字符串头像转换为 null，统一处理
      const avatarValue = payload.avatar && payload.avatar.trim() ? payload.avatar : null
      // 确保userId是字符串类型（防止精度丢失）
      const userId = typeof payload.userId === 'number' ? String(payload.userId) : payload.userId
      this.user = {
        userId: userId,
        username: payload.username,
        email: payload.email,
        avatar: avatarValue,
        roleType: payload.roleType,
        roles: payload.roles,
      }
    },
    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
    },
  },
})
