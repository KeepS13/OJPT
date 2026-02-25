// 校方相关类型定义

import type { ClassVO, ClassMemberVO } from './student'
import type { UserDetail } from './user'

// 学校VO
export interface SchoolVO {
  id: string
  name: string
  contact?: string
  status: number // 1启用/0禁用/2待认证
  certifiedAt?: string
  // 统计信息（可选）
  departmentCount?: number
  classCount?: number
  teacherCount?: number
  studentCount?: number
}

// 更新学校DTO
export interface SchoolUpdateDTO {
  name?: string
  contact?: string
  status?: number
}

// 创建学校DTO
export interface SchoolCreateDTO {
  name: string
  contact?: string
  status?: number
}

// 院系VO
export interface DepartmentVO {
  id: string
  schoolId: string
  name: string
  createdAt?: string
  updatedAt?: string
}

// 创建院系DTO
export interface DepartmentCreateDTO {
  name: string
}

// 更新院系DTO
export interface DepartmentUpdateDTO {
  name: string
}

// 教师VO
export interface TeacherVO {
  userId: string
  username: string
  email: string
  avatar?: string
  phone?: string
  studentNo?: string
  schoolId?: string
  roleType?: string
  roles?: string[]
}

// 添加教师DTO
export interface AddTeacherDTO {
  userId: string
}

// 统计概览
export interface SchoolStatisticsOverview {
  totalCount: number
  statusCount: {
    departments: number
    classes: number
    teachers: number
    students: number
  }
}

// 院系统计
export interface DepartmentStatistics {
  departmentId: string
  departmentName: string
  classCount: number
  studentCount: number
}

// 班级统计
export interface ClassStatistics {
  classId: string
  className: string
  studentCount: number
  teacherCount: number
}

// 导出其他类型
export type { ClassVO, ClassMemberVO, UserDetail }


