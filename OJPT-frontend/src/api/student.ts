import request from './request'
import type { PageParams, PageResponse } from './base'
import type { ClassVO, ClassMemberVO, ApplyResponse, QuitResponse } from '@/types/student'

// 获取我的班级列表（分页）
export function getMyClasses(params?: PageParams) {
  return request.get<PageResponse<ClassVO>>('/student/classes', { params })
}

// 获取班级详情
export function getClassDetail(classId: string) {
  return request.get<ClassVO>(`/student/classes/${classId}`)
}

// 申请加入班级
export function applyToClass(classId: string) {
  return request.post<ApplyResponse>(`/student/classes/${classId}/apply`)
}

// 退出班级
export function quitClass(classId: string) {
  return request.delete<QuitResponse>(`/student/classes/${classId}/quit`)
}

// 查看班级成员列表（分页）
export function getClassMembers(classId: string, params?: PageParams) {
  return request.get<PageResponse<ClassMemberVO>>(`/student/classes/${classId}/members`, { params })
}


