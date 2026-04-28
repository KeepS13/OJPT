import type { CurrentUser } from './auth'
import type { UserSubmissionRecord } from '@/api/user'

export interface UserDetail extends CurrentUser {
  phone?: string
  gender?: number
  birthday?: string
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

export type { UserSubmissionRecord }
