import type { RoleType } from '@/utils/role'

export interface AuthUser {
  userId: string
  username: string
  email: string
  avatar: string | null
  roleType: RoleType
  roles: RoleType[]
}

export interface AuthUserPayload {
  userId: string | number
  username: string
  email: string
  avatar: string | null
  roleType: RoleType | string
  roles: Array<RoleType | string>
}

export interface AuthTokens {
  tokenType: string
  accessToken: string
  expiresIn: number
  refreshToken: string
  refreshExpiresIn: number
}

export type LoginSuccessPayload = AuthUserPayload & AuthTokens

export interface CurrentUser extends AuthUserPayload {
  status: number
  createdAt: string
  updatedAt: string
}
