import request from './request'
import type { PageResponse } from './base'
import type {
  UserDetail,
  UserListParams,
  UserStatusUpdateDTO,
  UserRoleUpdateDTO,
  RoleVO,
  RoleCreateDTO,
  RoleUpdateDTO,
  PermissionVO,
  PermissionListParams,
  PermissionCreateDTO,
  PermissionUpdateDTO,
  RolePermissionAssignDTO,
  SchoolVO,
  SchoolListParams,
  SchoolCreateDTO,
  SchoolUpdateDTO,
  SchoolStatusUpdateDTO,
  PlatformStatisticsOverview,
  UserStatistics,
  SchoolStatistics,
  AdminProblemListItemVO,
  AdminProblemListParams,
  ProblemUpdateDTO,
  TagVO,
} from '@/types/admin'
import type { UserUpdateDTO } from './user'

// 获取用户列表（分页、筛选）
export function getUserList(params?: UserListParams) {
  return request.get<PageResponse<UserDetail>>('/admin/users', { params })
}

// 获取用户详情
export function getUserDetail(userId: string) {
  return request.get<UserDetail>(`/admin/users/${userId}`)
}

// 更新用户信息
export function updateUser(userId: string, payload: UserUpdateDTO) {
  return request.put<void>(`/admin/users/${userId}`, payload)
}

// 删除用户（软删除）
export function deleteUser(userId: string) {
  return request.delete<void>(`/admin/users/${userId}`)
}

// 修改用户状态
export function updateUserStatus(userId: string, payload: UserStatusUpdateDTO) {
  return request.put<void>(`/admin/users/${userId}/status`, payload)
}

// 修改用户角色绑定
export function updateUserRoles(userId: string, payload: UserRoleUpdateDTO) {
  return request.put<void>(`/admin/users/${userId}/roles`, payload)
}

// 获取角色列表
export function getRoleList() {
  return request.get<RoleVO[]>('/admin/roles')
}

// 创建角色
export function createRole(payload: RoleCreateDTO) {
  return request.post<RoleVO>('/admin/roles', payload)
}

// 获取角色详情（含权限列表）
export function getRoleDetail(roleId: string) {
  return request.get<RoleVO>(`/admin/roles/${roleId}`)
}

// 更新角色信息
export function updateRole(roleId: string, payload: RoleUpdateDTO) {
  return request.put<void>(`/admin/roles/${roleId}`, payload)
}

// 删除角色
export function deleteRole(roleId: string) {
  return request.delete<void>(`/admin/roles/${roleId}`)
}

// 获取权限列表
export function getPermissionList(params?: PermissionListParams) {
  return request.get<PermissionVO[]>('/admin/permissions', { params })
}

// 创建权限
export function createPermission(payload: PermissionCreateDTO) {
  return request.post<PermissionVO>('/admin/permissions', payload)
}

// 获取权限详情
export function getPermissionDetail(permissionId: string) {
  return request.get<PermissionVO>(`/admin/permissions/${permissionId}`)
}

// 更新权限
export function updatePermission(permissionId: string, payload: PermissionUpdateDTO) {
  return request.put<void>(`/admin/permissions/${permissionId}`, payload)
}

// 删除权限
export function deletePermission(permissionId: string) {
  return request.delete<void>(`/admin/permissions/${permissionId}`)
}

// 为角色分配权限
export function assignRolePermissions(roleId: string, payload: RolePermissionAssignDTO) {
  return request.post<void>(`/admin/roles/${roleId}/permissions`, payload)
}

// 移除角色权限
export function removeRolePermission(roleId: string, permissionId: string) {
  return request.delete<void>(
    `/admin/roles/${roleId}/permissions/${permissionId}`,
  )
}

// 获取学校列表（分页、筛选）
export function getSchoolList(params?: SchoolListParams) {
  return request.get<PageResponse<SchoolVO>>('/admin/schools', { params })
}

// 创建学校
export function createSchool(payload: SchoolCreateDTO) {
  return request.post<SchoolVO>('/admin/schools', payload)
}

// 获取学校详情
export function getSchoolDetail(schoolId: string) {
  return request.get<SchoolVO>(`/admin/schools/${schoolId}`)
}

// 更新学校信息
export function updateSchool(schoolId: string, payload: SchoolUpdateDTO) {
  return request.put<void>(`/admin/schools/${schoolId}`, payload)
}

// 删除学校
export function deleteSchool(schoolId: string) {
  return request.delete<void>(`/admin/schools/${schoolId}`)
}

// 修改学校状态
export function updateSchoolStatus(schoolId: string, payload: SchoolStatusUpdateDTO) {
  return request.put<void>(`/admin/schools/${schoolId}/status`, payload)
}

// 认证学校
export function certifySchool(schoolId: string) {
  return request.post<void>(`/admin/schools/${schoolId}/certify`)
}

// 取消认证
export function uncertifySchool(schoolId: string) {
  return request.delete<void>(`/admin/schools/${schoolId}/certify`)
}

// 获取平台整体数据概览
export function getPlatformStatisticsOverview() {
  return request.get<PlatformStatisticsOverview>('/admin/statistics/overview')
}

// 获取用户数据统计
export function getUserStatistics() {
  return request.get<UserStatistics>('/admin/statistics/users')
}

// 获取学校数据统计
export function getSchoolStatistics() {
  return request.get<SchoolStatistics>('/admin/statistics/schools')
}

// ========= 题库管理（Admin Problems）=========

export function getAdminProblemList(params?: AdminProblemListParams) {
  return request.get<PageResponse<AdminProblemListItemVO>>('/admin/problems', { params })
}

export function getAdminProblemDetail(problemId: string) {
  return request.get<unknown>(`/admin/problems/${problemId}`)
}

export function updateAdminProblem(problemId: string, payload: ProblemUpdateDTO) {
  return request.put<void>(`/admin/problems/${problemId}`, payload)
}

export function publishAdminProblem(problemId: string) {
  return request.post<void>(`/admin/problems/${problemId}:publish`)
}

export function archiveAdminProblem(problemId: string) {
  return request.post<void>(`/admin/problems/${problemId}:archive`)
}

export function getAdminTags() {
  return request.get<TagVO[]>('/admin/tags')
}

export function addTagToAdminProblem(problemId: string, tagId: string | number) {
  return request.post<void>(`/admin/problems/${problemId}/tags`, undefined, { params: { tagId } })
}

export function removeTagFromAdminProblem(problemId: string, tagId: string | number) {
  return request.delete<void>(`/admin/problems/${problemId}/tags`, { params: { tagId } })
}


