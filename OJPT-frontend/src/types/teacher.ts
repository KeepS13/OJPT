// 教师相关类型定义

import type { ClassVO, ClassMemberVO } from './student'

// 创建班级DTO
export interface ClassCreateDTO {
  departmentId: string
  name: string
  year?: string
  teacherId?: string
  merk?: string
}

// 更新班级DTO
export interface ClassUpdateDTO {
  name?: string
  year?: string
  teacherId?: string
  merk?: string
}

// 申请审核DTO
export interface ApplicationReviewDTO {
  reviewComment?: string
}

// 申请信息VO
export interface ApplicationVO {
  id: string
  classId: string
  className: string
  userId: string
  username: string
  email: string
  avatar?: string
  studentNo?: string
  joinType: 'APPLY' | 'INVITE'
  joinStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
  joinAt?: string
  reviewerId?: string
  reviewerName?: string
  reviewAt?: string
  reviewComment?: string
}

// 教师VO
export interface TeacherVO {
  teacherId: string
  username: string
  email: string
  avatar?: string
  role: string // 角色：班主任、助教等
  createdAt: string
}

// 导出ClassVO和ClassMemberVO供使用
export type { ClassVO, ClassMemberVO }


