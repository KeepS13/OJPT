import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useAuth } from '@/hooks/useAuth'
import { refreshToken } from './auth'
import { setTokens, clearTokens, getTokens } from '@/utils/storage'
import { getTokenExpiration } from '@/utils/jwt-utils'
import type { ApiResponse } from './base'

// accessToken 有效期 30 分钟，在剩余 5 分钟（即使用 25 分钟后）时主动刷新
const PROACTIVE_REFRESH_THRESHOLD_MS = 5 * 60 * 1000

let refreshTimerId: ReturnType<typeof setInterval> | null = null

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  transformResponse: [
    (data) => {
      // 确保大数字ID保持为字符串，避免精度丢失
      // 关键问题：JSON.parse在解析时会将大数字转换为number，如果超过MAX_SAFE_INTEGER会丢失精度
      // 解决方案：在JSON.parse之前，使用正则表达式将所有可能的ID字段值（大数字）替换为字符串
      if (typeof data === 'string') {
        try {
          // 使用正则表达式在JSON字符串中找到所有ID字段的大数字值，并替换为字符串
          // 匹配模式：": 1998338632572507002" 这样的格式，其中数字超过15位（可能是64位ID）
          // 匹配所有以id结尾的字段（不区分大小写）：userId, schoolId, classId, departmentId等
          let processedData = data

          // 匹配所有以id结尾的字段（包括单独的id字段），后面跟着15位以上的数字
          // 模式："(字段名id)": (大数字) 或 "id": (大数字)
          // 使用更精确的正则：匹配字段名以id结尾（不区分大小写），值是大数字
          // [^"]* 匹配0个或多个非引号字符，所以可以匹配 "id" 或 "schoolId" 等
          processedData = processedData.replace(
            /"([^"]*[Ii][Dd])"\s*:\s*(\d{15,})/g,
            (match, key, number) => {
              // 将大数字ID替换为字符串格式，确保精度不丢失
              return `"${key}":"${number}"`
            }
          )

          // 使用reviver作为双重保险，处理可能遗漏的情况
          const parsed = JSON.parse(processedData, (key, value) => {
            // 检查是否是ID字段且是数字类型
            if (
              value !== null &&
              typeof value === 'number' &&
              (key.toLowerCase().endsWith('id') || key === 'id')
            ) {
              // 所有ID字段都转换为字符串，确保64位ID精度不丢失
              return String(value)
            }
            return value
          })
          return parsed
        } catch {
          // 如果处理失败，尝试直接解析（可能数据格式不是标准JSON）
          try {
            return JSON.parse(data)
          } catch {
            return data
          }
        }
      }
      return data
    },
  ],
})

// 是否正在刷新 token
let isRefreshing = false
// 等待刷新完成的请求队列
let failedQueue: Array<{
  resolve: (value?: unknown) => void
  reject: (reason?: unknown) => void
}> = []

// 仅用于“主动刷新”场景的等待队列：等待刷新完成后继续发起请求
let refreshWaitQueue: Array<{
  resolve: () => void
  reject: (reason?: unknown) => void
}> = []

// 处理队列中的请求
const processQueue = (error: AxiosError | null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve()
    }
  })
  failedQueue = []
}

const processRefreshWaitQueue = (error: AxiosError | null) => {
  refreshWaitQueue.forEach((prom) => {
    if (error) prom.reject(error)
    else prom.resolve()
  })
  refreshWaitQueue = []
}

const shouldProactivelyRefresh = (accessToken: string) => {
  const expMs = getTokenExpiration(accessToken)
  if (!expMs) return false
  const remaining = expMs - Date.now()
  return remaining > 0 && remaining <= PROACTIVE_REFRESH_THRESHOLD_MS
}

// 后台定时器：定期检查token状态并主动刷新
const startTokenRefreshTimer = () => {
  if (refreshTimerId) {
    clearInterval(refreshTimerId)
  }

  refreshTimerId = setInterval(() => {
    const authStore = useAuthStore()

    if (!authStore.accessToken || !authStore.refreshToken || isRefreshing) {
      return
    }

    if (shouldProactivelyRefresh(authStore.accessToken)) {
      // 触发刷新（复用现有刷新逻辑）
      performTokenRefresh(authStore)
    }
  }, 60 * 1000) // 每分钟检查一次
}

// 执行token刷新的统一函数
const performTokenRefresh = async (authStore: ReturnType<typeof useAuthStore>) => {
  if (isRefreshing) return

  isRefreshing = true

  try {
    const res = await refreshToken({ refreshToken: authStore.refreshToken! })
    const { accessToken, refreshToken: newRefreshToken } = res.data

    // 只更新 token，保留现有用户信息
    authStore.setTokens(accessToken, newRefreshToken)
    setTokens(accessToken, newRefreshToken)

    // 刷新成功后调用 /auth/me 补全/更新用户信息（确保信息最新）
    try {
      const meRes = await request.get('/auth/me', {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      })
      if (meRes.data) {
        const avatarValue = meRes.data.avatar && meRes.data.avatar.trim() ? meRes.data.avatar : null
        const userId = typeof meRes.data.userId === 'number' ? String(meRes.data.userId) : meRes.data.userId
        authStore.setUserProfile({
          userId,
          username: meRes.data.username,
          email: meRes.data.email,
          avatar: avatarValue,
          roleType: meRes.data.roleType,
          roles: meRes.data.roles,
        })
      }
    } catch {
      // 获取用户信息失败不影响 token 刷新，静默失败
    }

    // 刷新成功后重新启动定时器
    startTokenRefreshTimer()
  } catch {
    authStore.clear()
    clearTokens()
    if (refreshTimerId) {
      clearInterval(refreshTimerId)
      refreshTimerId = null
    }
  } finally {
    isRefreshing = false
  }
}

// 导出启动定时器的函数，供外部调用（登录成功后）
export const ensureTokenRefreshTimer = () => {
  const authStore = useAuthStore()
  if (authStore.accessToken && authStore.refreshToken && !refreshTimerId) {
    startTokenRefreshTimer()
  }
}

// 导出停止定时器的函数，供外部调用（登出时）
export const stopTokenRefreshTimer = () => {
  if (refreshTimerId) {
    clearInterval(refreshTimerId)
    refreshTimerId = null
  }
}

// 初始化时启动定时器（如果已有token）
if (typeof window !== 'undefined') {
  // 延迟执行，确保 store 已初始化
  setTimeout(() => {
    ensureTokenRefreshTimer()
  }, 100)
}

request.interceptors.request.use(async (config) => {
  const authStore = useAuthStore()
  const auth = useAuth()

  // 若认证初始化尚未完成（自动登录进行中），等待其结束再继续
  if (auth.authInitializing) {
    // bootstrapAuth 在 useAuth 内部已启动，这里仅等待 authReady 变为 true
    // 简化处理：轮询等待一小段时间（避免引入额外依赖）
    await new Promise<void>((resolve) => {
      const check = () => {
        if (auth.authReady) {
          resolve()
        } else {
          setTimeout(check, 50)
        }
      }
      check()
    })
  }
  // 若 store 中没有令牌，尝试从本地存储恢复（应对仅保留 refreshToken 的场景）
  if (!authStore.accessToken && !authStore.refreshToken) {
    const cached = getTokens()
    if (cached.accessToken || cached.refreshToken) {
      authStore.setTokens(cached.accessToken || '', cached.refreshToken || '')
    }
  }

  // 主动刷新：accessToken 剩余时间 <= 29 分钟时，提前刷新，避免业务请求先打到 401
  // 刷新接口本身不触发主动刷新，避免递归
  const isRefreshRequest = !!config.url?.includes('/auth/refresh')
  if (!isRefreshRequest && authStore.accessToken && authStore.refreshToken && shouldProactivelyRefresh(authStore.accessToken)) {
    if (isRefreshing) {
      // 已有刷新在进行，等待完成
      await new Promise<void>((resolve, reject) => {
        refreshWaitQueue.push({ resolve, reject })
      })
    } else {
      await performTokenRefresh(authStore)
      // 处理等待队列（401 重试队列 + 主动刷新等待队列）
      processQueue(null)
      processRefreshWaitQueue(null)
    }
  }

  // 刷新 token 的请求不需要 accessToken
  if (authStore.accessToken && !config.url?.includes('/auth/refresh')) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as unknown
    // 后端统一响应结构：{ code, message, data, timestamp }
    if (body && typeof body === 'object' && 'code' in body && 'message' in body && 'data' in body) {
      const api = body as ApiResponse<unknown>
      if (api.code !== 200) {
        // 正常情况下后端会用 HTTP 状态码标识错误；这里兜底处理“200 但 code!=200”的情况
        ElMessage.error(api.message || '请求失败')
        return Promise.reject(new Error(api.message || '请求失败'))
      }
      // 解包：让业务侧继续使用 res.data 直接拿到 data
      ;(response as unknown as { data: unknown }).data = api.data
    }
    return response
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    const status = error.response?.status
    const canRetry =
      status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh')

    // 后端统一处理：所有 token 过期/无效的情况都返回 401
    // 只有 401 时尝试刷新 token
    // 403 在后端语义为账号被封禁或权限不足，直接按业务错误处理，不尝试刷新
    if (canRetry) {
      const authStore = useAuthStore()

      // 如果正在刷新 token，将请求加入队列等待
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: () => {
              // 刷新完成后，使用新的 token 重试请求
              originalRequest.headers = originalRequest.headers || {}
              originalRequest.headers.Authorization = `Bearer ${authStore.accessToken}`
              request(originalRequest).then(resolve).catch(reject)
            },
            reject,
          })
        })
      }

      // 如果没有 refreshToken，尝试从 localStorage 恢复
      if (!authStore.refreshToken) {
        const cached = getTokens()
        if (cached.refreshToken) {
          // 确保类型安全：localStorage.getItem 可能返回 null
          const accessToken: string = cached.accessToken ?? ''
          const refreshTokenValue: string = cached.refreshToken ?? ''
          authStore.setTokens(accessToken, refreshTokenValue)
        } else {
          // localStorage 中也没有 refreshToken，清除并拒绝
          authStore.clear()
          clearTokens()
          return Promise.reject(error)
        }
      }

      // 标记正在刷新，避免重复刷新
      originalRequest._retry = true
      isRefreshing = true

      try {
        // 尝试刷新 token（401 被动刷新）
        // 确保 refreshToken 不为 null（前面已经检查过）
        const refreshTokenValue = authStore.refreshToken
        if (!refreshTokenValue) {
          throw new Error('Refresh token is missing')
        }
        const res = await refreshToken({ refreshToken: refreshTokenValue })
        const { accessToken, refreshToken: newRefreshToken, ...userData } = res.data

        // 确保userId是字符串类型（防止精度丢失）
        const userId = typeof userData.userId === 'number' ? String(userData.userId) : userData.userId

        // 更新 store 和 localStorage
        authStore.setFromLogin({
          accessToken,
          refreshToken: newRefreshToken,
          ...userData,
          userId,
        })
        setTokens(accessToken, newRefreshToken)

        // 刷新 token 成功后，主动调用 /auth/me 获取最新的用户信息（包括头像）
        // 这样可以确保头像信息是最新的，避免刷新 token 响应中头像信息不完整的问题
        // 异步调用，不阻塞当前请求
        request.get('/auth/me', {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        })
          .then((meRes) => {
            if (authStore.user && meRes.data) {
              // 将空字符串头像转换为 null，统一处理
              const latestAvatar = meRes.data.avatar && meRes.data.avatar.trim() ? meRes.data.avatar : null
              // 确保userId是字符串类型
              const userId = typeof meRes.data.userId === 'number' ? String(meRes.data.userId) : meRes.data.userId
              // 只更新用户信息，不更新 token（token 已经更新过了）
              authStore.setUserProfile({
                userId: userId,
                username: meRes.data.username,
                email: meRes.data.email,
                avatar: latestAvatar,
                roleType: meRes.data.roleType,
                roles: meRes.data.roles,
              })
            }
          })
          .catch(() => {
            // 获取用户信息失败不影响 token 刷新，静默失败
          })

        // 处理等待队列
        processQueue(null)

        // 使用新的 token 重试原始请求
        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return request(originalRequest)
      } catch (refreshError) {
        const refreshAxiosError = refreshError as AxiosError<{ message?: string }>
        const refreshStatus = refreshAxiosError.response?.status

        // 刷新失败的处理：
        // - 401：refreshToken 过期/无效，清理登录态
        // - 403：账号被封禁，清理登录态
        // - 其他：网络错误等，也清理登录态
        processQueue(refreshAxiosError)
        authStore.clear()
        clearTokens()

        // 如果是 refreshToken 过期（401），提示用户登录验证已过期
        if (refreshStatus === 401) {
          ElMessage.warning('登录验证已过期，请重新登录')
          // 延时跳转，确保用户能看到提示信息
          if (typeof window !== 'undefined') {
            setTimeout(() => {
              window.location.href = '/'
            }, 1500) // 1.5秒后跳转
          }
        } else {
        // 如果是账号被封禁（403），可以在这里显示更友好的提示
        // 但通常会在业务层（如 LoginDialog）处理封禁提示
        if (typeof window !== 'undefined') {
          window.location.href = '/'
          }
        }
        return Promise.reject(refreshAxiosError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default request
