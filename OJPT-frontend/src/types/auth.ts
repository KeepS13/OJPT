// 登录 / 刷新后返回的用户与 token 信息

export interface AuthUser {
  userId: string
  username: string
  email: string
  avatar: string | null
  roleType: string
  roles: string[]
}

export interface AuthTokens {
  tokenType: string
  accessToken: string
  expiresIn: number
  refreshToken: string
  refreshExpiresIn: number
}

// 后端 LoginResponseVO 结构
export type LoginSuccessPayload = AuthUser & AuthTokens

// /auth/me 返回的当前用户信息（不含 token）
export interface CurrentUser {
  userId: string
  username: string
  email: string
  avatar: string | null
  roleType: string
  status: number
  roles: string[]
  createdAt: string
  updatedAt: string
}


