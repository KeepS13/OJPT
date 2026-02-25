import request from './request'
import type { UserDetail } from '@/types/user'

// 获取当前用户详细信息（含扩展信息）
export function getCurrentUserDetail() {
  // 对应 API 文档：GET /api/users/me/detail
  return request.get<UserDetail>('/users/me/detail')
}

// 更新个人信息请求体
export interface UserUpdateDTO {
  email?: string
  phone?: string
  gender?: number // 0未知/1男/2女
  birthday?: string // YYYY-MM-DD
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

// 更新个人信息响应
export interface UserUpdateResponse {
  message: string
}

// 更新个人信息
export function updateUserInfo(payload: UserUpdateDTO) {
  return request.put<UserUpdateResponse>('/users/me', payload)
}

// 上传头像响应
export interface AvatarUploadResponse {
  message: string
  avatar: string
}

// 上传头像
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<AvatarUploadResponse>('/users/me/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

// 删除头像
export function deleteAvatar() {
  // 根据 API 文档，删除头像时不传 file 字段，由后端根据空表单识别为删除操作
  const formData = new FormData()
  return request.post<AvatarUploadResponse>('/users/me/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

// 账号安全相关接口

// 修改用户名请求体
export interface UpdateUsernameDTO {
  username: string
}

// 修改邮箱请求体
export interface UpdateEmailDTO {
  email: string
}

// 修改手机号请求体
export interface UpdatePhoneDTO {
  phone: string
}

// 修改密码请求体
export interface UpdatePasswordDTO {
  oldPassword: string
  newPassword: string
}

// 修改用户名
export function updateUsername(payload: UpdateUsernameDTO) {
  return request.put<UserUpdateResponse>('/users/me/username', payload)
}

// 修改邮箱
export function updateEmail(payload: UpdateEmailDTO) {
  return request.put<UserUpdateResponse>('/users/me/email', payload)
}

// 修改手机号
export function updatePhone(payload: UpdatePhoneDTO) {
  return request.put<UserUpdateResponse>('/users/me/phone', payload)
}

// 修改密码
export function updatePassword(payload: UpdatePasswordDTO) {
  return request.put<UserUpdateResponse>('/users/me/password', payload)
}

// 注销账号
export function deleteAccount() {
  return request.delete<UserUpdateResponse>('/users/me')
}

