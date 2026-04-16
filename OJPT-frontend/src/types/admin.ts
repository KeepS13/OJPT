// 管理员相关类型定义

import type { UserDetail } from './user'
import type { SchoolVO, SchoolCreateDTO, SchoolUpdateDTO } from './school'

// 用户列表查询参数
export interface UserListParams {
  page?: number
  size?: number
  status?: number // 0禁用/1启用/2待审核
  roleType?: 'USER' | 'STUDENT' | 'TEACHER' | 'SCHOOL' | 'ADMIN'
  keyword?: string // 搜索关键词
}

// 用户状态更新DTO
export interface UserStatusUpdateDTO {
  status: number // 0禁用/1启用/2待审核
}

// 用户角色更新DTO
export interface UserRoleUpdateDTO {
  roleCodes: string[]
}

// 角色VO
export interface RoleVO {
  id: string
  code: string
  name: string
  description?: string
  level: number
  permissionCount?: number
  permissions?: PermissionVO[]
  createdAt: string
  updatedAt: string
}

// 创建角色DTO
export interface RoleCreateDTO {
  code: string
  name: string
  description?: string
  level?: number
}

// 更新角色DTO
export interface RoleUpdateDTO {
  name?: string
  description?: string
  level?: number
}

// 权限VO
export interface PermissionVO {
  id: string
  resource: string
  action: string
  conditionJson?: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

// 权限查询参数
export interface PermissionListParams {
  resource?: string
  action?: string
  keyword?: string
}

// 创建权限DTO
export interface PermissionCreateDTO {
  resource: string
  action: string
  conditionJson?: string
  description?: string
}

// 更新权限DTO
export interface PermissionUpdateDTO {
  conditionJson?: string
  description?: string
}

// 角色权限分配DTO
export interface RolePermissionAssignDTO {
  permissionIds: string[]
}

// 学校列表查询参数
export interface SchoolListParams {
  page?: number
  size?: number
  status?: number // 1启用/0禁用/2待认证
  keyword?: string
}

// 学校状态更新DTO
export interface SchoolStatusUpdateDTO {
  status: number // 1启用/0禁用/2待认证
}

// 平台统计概览
export interface PlatformStatisticsOverview {
  totalCount: number
  statusCount: {
    users: number
    schools: number
  }
}

// 用户数据统计
export interface UserStatistics {
  totalCount: number
  statusCount: {
    [key: string]: number // "0", "1", "2"
  }
  roleCount: {
    USER: number
    STUDENT: number
    TEACHER: number
    SCHOOL: number
    ADMIN: number
  }
}

// 学校数据统计
export interface SchoolStatistics {
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

// 导出其他类型
export type { UserDetail, SchoolVO, SchoolCreateDTO, SchoolUpdateDTO }


