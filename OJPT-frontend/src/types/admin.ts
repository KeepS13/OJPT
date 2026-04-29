import type { UserDetail } from './user'

export interface UserListParams {
  page?: number
  size?: number
  status?: number
  roleType?: 'USER' | 'ADMIN'
  keyword?: string
}

export interface UserStatusUpdateDTO {
  status: number
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

export interface PlatformStatisticsOverview {
  totalCount: number
  statusCount: {
    users: number
  }
}

export interface UserStatistics {
  totalCount: number
  statusCount: Record<string, number>
}

export type JudgeEnvironmentCheckStatus = 'UP' | 'DOWN' | 'SKIPPED'

export interface JudgeEnvironmentCheckDTO {
  name: string
  status: JudgeEnvironmentCheckStatus
  target: string
  message: string
}

export interface JudgeEnvironmentHealthDTO {
  status: JudgeEnvironmentCheckStatus
  message: string
  checks: JudgeEnvironmentCheckDTO[]
}

export type ProblemDifficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type ProblemPublishStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface TagVO {
  id: string
  name: string
  type?: string | null
}

export interface TagCreateDTO {
  name: string
  type?: string
}

export interface TagUpdateDTO {
  name?: string
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
