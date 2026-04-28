import { defineStore } from 'pinia'
import type { AuthUser, AuthUserPayload, LoginSuccessPayload } from '@/types/auth'
import { normalizeRoles, normalizeRoleType } from '@/utils/role'

type UserProfilePayload = AuthUserPayload

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
    setUserProfile(payload: UserProfilePayload) {
      const avatarValue = payload.avatar && payload.avatar.trim() ? payload.avatar : null
      const userId = typeof payload.userId === 'number' ? String(payload.userId) : payload.userId
      this.user = {
        userId,
        username: payload.username,
        email: payload.email,
        avatar: avatarValue,
        roleType: normalizeRoleType(payload.roleType, payload.roles),
        roles: normalizeRoles(payload.roleType, payload.roles),
      }
    },
    setFromLogin(payload: LoginSuccessPayload) {
      this.accessToken = payload.accessToken
      this.refreshToken = payload.refreshToken
      this.setUserProfile(payload)
    },
    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
    },
  },
})
