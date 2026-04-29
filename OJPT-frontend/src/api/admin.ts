import type { PageResponse } from './base'
import request from './request'
import type { UserUpdateDTO } from './user'
import type {
  AdminProblemListItemVO,
  AdminProblemListParams,
  JudgeEnvironmentHealthDTO,
  PasswordResetRequestStatus,
  PasswordResetRequestVO,
  PlatformStatisticsOverview,
  ProblemCreateDTO,
  ProblemTestCaseBatchUpdateDTO,
  ProblemTestCaseVO,
  ProblemUpdateDTO,
  TagCreateDTO,
  TagUpdateDTO,
  TagVO,
  UserDetail,
  UserListParams,
  UserStatistics,
  UserStatusUpdateDTO,
} from '@/types/admin'

export function getUserList(params?: UserListParams) {
  return request.get<PageResponse<UserDetail>>('/admin/users', { params })
}

export function getUserDetail(userId: string | number) {
  return request.get<UserDetail>(`/admin/users/${userId}`)
}

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
  return request.get<PasswordResetRequestVO[]>('/admin/password-reset-requests', {
    params: { status },
  })
}

export function approvePasswordResetRequest(requestId: string | number) {
  return request.post<void>(`/admin/password-reset-requests/${requestId}:approve`)
}

export function rejectPasswordResetRequest(requestId: string | number) {
  return request.post<void>(`/admin/password-reset-requests/${requestId}:reject`)
}

export function getPlatformStatisticsOverview() {
  return request.get<PlatformStatisticsOverview>('/admin/statistics/overview')
}

export function getUserStatistics() {
  return request.get<UserStatistics>('/admin/statistics/users')
}

export function getJudgeEnvironmentHealth() {
  return request.get<JudgeEnvironmentHealthDTO>('/admin/judge-environment/health')
}

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

export function createAdminTag(payload: TagCreateDTO) {
  return request.post<TagVO>('/admin/tags', payload)
}

export function updateAdminTag(tagId: string | number, payload: TagUpdateDTO) {
  return request.put<void>(`/admin/tags/${tagId}`, payload)
}

export function deleteAdminTag(tagId: string | number) {
  return request.delete<void>(`/admin/tags/${tagId}`)
}

export function addTagToAdminProblem(problemId: string, tagId: string | number) {
  return request.post<void>(`/admin/problems/${problemId}/tags`, undefined, {
    params: { tagId },
  })
}

export function removeTagFromAdminProblem(problemId: string, tagId: string | number) {
  return request.delete<void>(`/admin/problems/${problemId}/tags`, { params: { tagId } })
}
