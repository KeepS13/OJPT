import request from './request'
import type { PageParams, PageResponse } from './base'
import type {
  ClassVO,
  ClassMemberVO,
  ClassCreateDTO,
  ClassUpdateDTO,
  ApplicationVO,
  ApplicationReviewDTO,
  TeacherVO,
} from '@/types/teacher'

// 获取我管理的班级列表（分页）
export function getMyManagedClasses(params?: PageParams) {
  return request.get<PageResponse<ClassVO>>('/teacher/classes', { params })
}

// 创建班级
export function createClass(payload: ClassCreateDTO) {
  return request.post<ClassVO>('/teacher/classes', payload)
}

// 获取班级详情
export function getClassDetail(classId: string) {
  return request.get<ClassVO>(`/teacher/classes/${classId}`)
}

// 更新班级信息
export function updateClass(classId: string, payload: ClassUpdateDTO) {
  return request.put<ClassVO>(`/teacher/classes/${classId}`, payload)
}

// 删除班级
export function deleteClass(classId: string) {
  return request.delete<{ message: string }>(`/teacher/classes/${classId}`)
}

// 获取班级学员列表（已通过审核的）（分页）
export function getClassStudents(classId: string, params?: PageParams) {
  return request.get<PageResponse<ClassMemberVO>>(`/teacher/classes/${classId}/students`, { params })
}

// 获取加入申请列表（待审核）（分页）
export function getClassApplications(classId: string, params?: PageParams) {
  return request.get<PageResponse<ApplicationVO>>(`/teacher/classes/${classId}/applications`, { params })
}

// 批准加入申请
export function approveApplication(
  classId: string,
  applicationId: string,
  payload?: ApplicationReviewDTO,
) {
  return request.post<{ message: string }>(
    `/teacher/classes/${classId}/applications/${applicationId}/approve`,
    payload,
  )
}

// 拒绝加入申请
export function rejectApplication(
  classId: string,
  applicationId: string,
  payload?: ApplicationReviewDTO,
) {
  return request.post<{ message: string }>(
    `/teacher/classes/${classId}/applications/${applicationId}/reject`,
    payload,
  )
}

// 邀请学员加入
export function inviteStudent(classId: string, studentId: string) {
  return request.post<{ message: string }>(
    `/teacher/classes/${classId}/students/${studentId}/invite`,
  )
}

// 移除学员
export function removeStudent(classId: string, studentId: string) {
  return request.delete<{ message: string }>(
    `/teacher/classes/${classId}/students/${studentId}`,
  )
}

// 获取班级的教师列表（分页）
export function getClassTeachers(classId: string, params?: PageParams) {
  return request.get<PageResponse<TeacherVO>>(`/teacher/classes/${classId}/teachers`, { params })
}

// 添加教师到班级
export function addTeacherToClass(classId: string, teacherId: string, role?: string) {
  const params = new URLSearchParams()
  params.append('teacherId', teacherId)
  if (role) {
    params.append('role', role)
  }
  return request.post<{ message: string }>(
    `/teacher/classes/${classId}/teachers?${params.toString()}`,
  )
}

// 移除班级教师
export function removeTeacherFromClass(classId: string, teacherId: string) {
  return request.delete<{ message: string }>(
    `/teacher/classes/${classId}/teachers/${teacherId}`,
  )
}


