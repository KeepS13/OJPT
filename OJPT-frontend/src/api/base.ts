import type { AxiosError, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一API响应格式
 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

/**
 * 分页请求参数
 */
export interface PageParams {
  page?: number
  size?: number
}

/**
 * 分页响应格式
 */
export interface PageResponse<T = unknown> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/**
 * 统一错误处理
 *
 * @param error 错误对象
 * @param showMessage 是否显示错误消息（默认true）
 * @returns never 总是抛出错误
 */
export function handleApiError(error: unknown, showMessage = true): never {
  const err = error as AxiosError<ApiResponse>

  let message = '请求失败'

  if (err.response) {
    // 服务器返回了错误响应
    const responseData = err.response.data
    if (responseData && typeof responseData === 'object' && 'message' in responseData) {
      message = responseData.message as string
    } else if (err.response.status === 401) {
      message = '未授权，请重新登录'
    } else if (err.response.status === 403) {
      message = '权限不足'
    } else if (err.response.status === 404) {
      message = '资源不存在'
    } else if (err.response.status === 500) {
      message = '服务器内部错误'
    }
  } else if (err.request) {
    // 请求已发出但没有收到响应
    message = '网络错误，请检查网络连接'
  } else if (err.message) {
    // 其他错误
    message = err.message
  }

  if (showMessage) {
    ElMessage.error(message)
  }

  throw error
}

/**
 * 提取API响应数据
 * 兼容新旧两种响应格式
 */
export function extractApiData<T>(response: AxiosResponse<ApiResponse<T> | T>): T {
  const data = response.data

  // 如果是统一响应格式（有code字段）
  if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
    const apiResponse = data as ApiResponse<T>
    if (apiResponse.code === 200) {
      return apiResponse.data
    } else {
      throw new Error(apiResponse.message || '请求失败')
    }
  }

  // 旧格式：直接返回数据
  return data as T
}

/**
 * 检查响应是否成功
 */
export function isSuccessResponse(response: ApiResponse): boolean {
  return response.code === 200
}
