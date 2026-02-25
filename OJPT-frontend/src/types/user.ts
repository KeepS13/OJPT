import type { CurrentUser } from './auth'

// 用户详细信息（包含扩展信息）
export interface UserDetail extends CurrentUser {
  phone?: string
  gender?: number // 0未知/1男/2女
  birthday?: string // YYYY-MM-DD
  address?: string
  website?: string
  github?: string
  company?: string
  position?: string
  skills?: string
  studentNo?: string
  schoolId?: string
  bio?: string
  tags?: string
}

