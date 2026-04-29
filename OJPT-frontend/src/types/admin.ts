// 管理员相关类型定义
import type { UserDetail } from './user'

// 用户列表查询参数
export interface UserListParams {
  page?: number
  size?: number
  status?: number // 0 禁用 / 1 启用 / 2 待审核
  roleType?: 'USER' | 'ADMIN'
  keyword?: string // 搜索关键字
}

// 用户状态更新 DTO
export interface UserStatusUpdateDTO {
  status: number // 0 禁用 / 1 启用 / 2 待审核
}

export type PasswordResetRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface PasswordResetRequestVO {
  id: string
  userId: string
  username?: string | null
  email?: string | null
  accountIdentifier: string
  status: PasswordResetRequestStatus
  reviewedBy?: string | null
  reviewedAt?: string | null
  createdAt?: string | null
}

// 平台统计概览
export interface PlatformStatisticsOverview {
  totalCount: number
  statusCount: {
    users: number
  }
}

// 用户数据统计
export interface UserStatistics {
  totalCount: number
  statusCount: {
    [key: string]: number // "0", "1", "2"
  }
}

// ========== 题库管理（Admin Problems）==========

export type ProblemDifficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type ProblemPublishStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface TagVO {
  id: string
  name: string
  type?: string
}

export interface AdminProblemListParams {
  page?: number
  size?: number
  keyword?: string
  difficulty?: ProblemDifficulty
  tagId?: string | number
  status?: ProblemPublishStatus
  orderBy?: string
}

export interface AdminProblemListItemVO {
  id: string
  problemNo?: number | null
  title: string
  difficulty: ProblemDifficulty
  status: ProblemPublishStatus
  submitCount?: number | null
  acceptedCount?: number | null
  acceptanceRate?: number | null
  tags?: TagVO[] | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ProblemUpdateDTO {
  title?: string
  difficulty?: ProblemDifficulty
  statementMd?: string
  timeLimitMs?: number | null
  memoryLimitKb?: number | null
}

export interface ProblemCreateDTO {
  title: string
  difficulty: ProblemDifficulty
  statementMd: string
  timeLimitMs: number
  memoryLimitKb: number
}

export type ProblemTestCaseType = 'SAMPLE' | 'HIDDEN'

export interface ProblemTestCaseVO {
  id?: string
  caseType: ProblemTestCaseType
  sortOrder: number
  inputText: string
  expectedOutput: string
  explanation?: string | null
}

export interface ProblemTestCaseBatchUpdateDTO {
  cases: ProblemTestCaseVO[]
}

export type { UserDetail }
