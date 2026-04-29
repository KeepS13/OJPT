import request from './request'
import type { PageResponse } from './base'
import type {
  UserDetail,
  UserListParams,
  UserStatusUpdateDTO,
  PasswordResetRequestStatus,
  PasswordResetRequestVO,
  PlatformStatisticsOverview,
  UserStatistics,
  AdminProblemListItemVO,
  AdminProblemListParams,
  ProblemCreateDTO,
  ProblemUpdateDTO,
  TagVO,
  ProblemTestCaseVO,
  ProblemTestCaseBatchUpdateDTO,
} from '@/types/admin'
import type { UserUpdateDTO } from './user'

// 获取用户列表（分页、筛选）
export function getUserList(params?: UserListParams) {
  return request.get<PageResponse<UserDetail>>('/admin/users', { params })
}

// 获取用户详情
export function getUserDetail(userId: string | number) {
  return request.get<UserDetail>(`/admin/users/${userId}`)
}

// 更新用户信息
export function updateUser(userId: string | number, payload: UserUpdateDTO) {
  return request.put<void>(`/admin/users/${userId}`, payload)
}

export function deleteUser(userId: string | number) {
  return request.delete<void>(`/admin/users/${userId}`)
}

export function updateUserStatus(userId: string | number, payload: UserStatusUpdateDTO) {
  return request.put<void>(`/admin/users/${userId}/status`, payload)
}

export function getPasswordResetRequests(status: PasswordResetRequestStatus = 'PENDING') {
  return request.get<PasswordResetRequestVO[]>('/admin/password-reset-requests', { params: { status } })
}

export function approvePasswordResetRequest(requestId: string | number) {
  return request.post<void>(`/admin/password-reset-requests/${requestId}:approve`)
}

export function rejectPasswordResetRequest(requestId: string | number) {
  return request.post<void>(`/admin/password-reset-requests/${requestId}:reject`)
}

// 获取平台整体数据概览
export function getPlatformStatisticsOverview() {
  return request.get<PlatformStatisticsOverview>('/admin/statistics/overview')
}

// 获取用户数据统计
export function getUserStatistics() {
  return request.get<UserStatistics>('/admin/statistics/users')
}

// ========= 题库管理（Admin Problems）=========

export function getAdminProblemList(params?: AdminProblemListParams) {
  return request.get<PageResponse<AdminProblemListItemVO>>('/admin/problems', { params })
}

export function getAdminProblemDetail(problemId: string) {
  return request.get<unknown>(`/admin/problems/${problemId}`)
}

export function createAdminProblem(payload: ProblemCreateDTO) {
  return request.post<{ id: string }>('/admin/problems', payload)
}

export function getAdminProblemTestCases(problemId: string) {
  return request.get<ProblemTestCaseVO[]>(`/admin/problems/${problemId}/test-cases`)
}

export function replaceAdminProblemTestCases(
  problemId: string,
  payload: ProblemTestCaseBatchUpdateDTO,
) {
  return request.put<void>(`/admin/problems/${problemId}/test-cases`, payload)
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
