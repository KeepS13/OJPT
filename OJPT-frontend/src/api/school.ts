import request from './request'
import type { PageParams, PageResponse } from './base'
import type {
  SchoolVO,
  SchoolUpdateDTO,
  DepartmentVO,
  DepartmentCreateDTO,
  DepartmentUpdateDTO,
  TeacherVO,
  AddTeacherDTO,
  ClassVO,
  ClassMemberVO,
  UserDetail,
  SchoolStatisticsOverview,
  DepartmentStatistics,
  ClassStatistics,
} from '@/types/school'
import type { UserUpdateDTO } from './user'

// 获取当前校方管理的学校信息
export function getSchoolInfo() {
  return request.get<SchoolVO>('/school/info')
}

// 更新学校信息
export function updateSchoolInfo(payload: SchoolUpdateDTO) {
  return request.put<SchoolVO>('/school/info', payload)
}

// 获取学校认证状态
export function getSchoolCertification() {
  return request.get<SchoolVO>('/school/certification')
}

// 获取院系列表（分页）
export function getDepartments(params?: PageParams) {
  return request.get<PageResponse<DepartmentVO>>('/school/departments', { params })
}

// 创建院系
export function createDepartment(payload: DepartmentCreateDTO) {
  return request.post<DepartmentVO>('/school/departments', payload)
}

// 获取院系详情
export function getDepartmentDetail(departmentId: string) {
  return request.get<DepartmentVO>(`/school/departments/${departmentId}`)
}

// 更新院系信息
export function updateDepartment(departmentId: string, payload: DepartmentUpdateDTO) {
  return request.put<DepartmentVO>(`/school/departments/${departmentId}`, payload)
}

// 删除院系
export function deleteDepartment(departmentId: string) {
  return request.delete<{ message: string }>(`/school/departments/${departmentId}`)
}

// 获取学校下所有班级列表（分页）
export function getSchoolClasses(params?: PageParams) {
  return request.get<PageResponse<ClassVO>>('/school/classes', { params })
}

// 获取指定院系下的班级列表（分页）
export function getDepartmentClasses(departmentId: string, params?: PageParams) {
  return request.get<PageResponse<ClassVO>>(`/school/departments/${departmentId}/classes`, { params })
}

// 获取班级详情
export function getClassDetail(classId: string) {
  return request.get<ClassVO>(`/school/classes/${classId}`)
}

// 更新班级信息
export function updateClass(classId: string, payload: { name?: string; year?: string; teacherId?: string; merk?: string }) {
  return request.put<ClassVO>(`/school/classes/${classId}`, payload)
}

// 删除班级
export function deleteClass(classId: string) {
  return request.delete<{ message: string }>(`/school/classes/${classId}`)
}

// 获取学校下所有教师列表（分页）
export function getSchoolTeachers(params?: PageParams) {
  return request.get<PageResponse<TeacherVO>>('/school/teachers', { params })
}

// 添加教师
export function addTeacher(payload: AddTeacherDTO) {
  return request.post<TeacherVO>('/school/teachers', payload)
}

// 获取教师详情
export function getTeacherDetail(teacherId: string) {
  return request.get<TeacherVO>(`/school/teachers/${teacherId}`)
}

// 更新教师信息
export function updateTeacher(teacherId: string, payload: UserUpdateDTO) {
  return request.put<TeacherVO>(`/school/teachers/${teacherId}`, payload)
}

// 移除教师角色
export function removeTeacher(teacherId: string) {
  return request.delete<{ message: string }>(`/school/teachers/${teacherId}`)
}

// 获取教师管理的班级列表（分页）
export function getTeacherClasses(teacherId: string, params?: PageParams) {
  return request.get<PageResponse<ClassVO>>(`/school/teachers/${teacherId}/classes`, { params })
}

// 获取学校下所有学员列表（分页）
export function getSchoolStudents(params?: PageParams) {
  return request.get<PageResponse<UserDetail>>('/school/students', { params })
}

// 获取指定院系的学员列表（分页）
export function getDepartmentStudents(departmentId: string, params?: PageParams) {
  return request.get<PageResponse<UserDetail>>(`/school/departments/${departmentId}/students`, { params })
}

// 获取指定班级的学员列表（分页）
export function getClassStudents(classId: string, params?: PageParams) {
  return request.get<PageResponse<ClassMemberVO>>(`/school/classes/${classId}/students`, { params })
}

// 获取学员详情
export function getStudentDetail(studentId: string) {
  return request.get<UserDetail>(`/school/students/${studentId}`)
}

// 更新学员信息
export function updateStudent(studentId: string, payload: UserUpdateDTO) {
  return request.put<UserDetail>(`/school/students/${studentId}`, payload)
}

// 获取学校整体数据概览
export function getSchoolStatisticsOverview() {
  return request.get<SchoolStatisticsOverview>('/school/statistics/overview')
}

// 获取各院系数据统计
export function getDepartmentStatistics() {
  return request.get<DepartmentStatistics[]>('/school/statistics/departments')
}

// 获取各班级数据统计
export function getClassStatistics() {
  return request.get<ClassStatistics[]>('/school/statistics/classes')
}


