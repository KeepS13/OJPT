import request from './request'
import type { PageParams, PageResponse } from './base'
import type { UserDetail } from '@/types/user'

export function getCurrentUserDetail() {
  return request.get<UserDetail>('/users/me/detail')
}

export interface TrainingDashboardRecentSubmission {
  submissionId: string
  problemId: string
  problemNo?: number | null
  problemTitle?: string | null
  language: string
  status: string
  timeMs?: number | null
  memoryKb?: number | null
  createdAt: string
}

export interface UserTrainingDashboard {
  totalSubmissions: number
  acceptedSubmissions: number
  solvedProblemCount: number
  acceptanceRate: number
  recentSubmissions: TrainingDashboardRecentSubmission[]
  statusDistribution: Record<string, number>
  difficultyDistribution: Record<string, number>
}

export function getCurrentUserTrainingDashboard() {
  return request.get<UserTrainingDashboard>('/users/me/training-dashboard')
}

export interface UserSubmissionRecord {
  submissionId: string
  problemId: string
  problemNo?: number | null
  problemTitle?: string | null
  language: string
  status: string
  sourceCode: string
  timeMs?: number | null
  memoryKb?: number | null
  compileMessage?: string | null
  judgeMessage?: string | null
  createdAt: string
}

export function getCurrentUserSubmissionRecords(params?: PageParams) {
  return request.get<PageResponse<UserSubmissionRecord>>('/users/me/submissions', { params })
}

export interface UserUpdateDTO {
  email?: string
  phone?: string
  gender?: number
  birthday?: string
  address?: string
  website?: string
  github?: string
  company?: string
  position?: string
  skills?: string
  studentNo?: string
  schoolId?: string
  bio?: string
  tags?: string
}

export interface UserUpdateResponse {
  message: string
}

export function updateUserInfo(payload: UserUpdateDTO) {
  return request.put<UserUpdateResponse>('/users/me', payload)
}

export interface AvatarUploadResponse {
  message: string
  avatar: string
}

export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<AvatarUploadResponse>('/users/me/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export function deleteAvatar() {
  const formData = new FormData()
  return request.post<AvatarUploadResponse>('/users/me/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export interface UpdateUsernameDTO {
  username: string
}

export interface UpdateEmailDTO {
  email: string
}

export interface UpdatePhoneDTO {
  phone: string
}

export interface UpdatePasswordDTO {
  oldPassword: string
  newPassword: string
}

export function updateUsername(payload: UpdateUsernameDTO) {
  return request.put<UserUpdateResponse>('/users/me/username', payload)
}

export function updateEmail(payload: UpdateEmailDTO) {
  return request.put<UserUpdateResponse>('/users/me/email', payload)
}

export function updatePhone(payload: UpdatePhoneDTO) {
  return request.put<UserUpdateResponse>('/users/me/phone', payload)
}

export function updatePassword(payload: UpdatePasswordDTO) {
  return request.put<UserUpdateResponse>('/users/me/password', payload)
}

export function deleteAccount() {
  return request.delete<UserUpdateResponse>('/users/me')
}
