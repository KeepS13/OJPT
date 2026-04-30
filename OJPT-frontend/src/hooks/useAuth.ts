import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { clearTokens, setTokens, getTokens } from '@/utils/storage'
import { logout as apiLogout, getCurrentUser, refreshToken as apiRefreshToken } from '@/api/auth'
import { ensureTokenRefreshTimer, stopTokenRefreshTimer } from '@/api/request'
import type { CurrentUser, LoginSuccessPayload } from '@/types/auth'
import type { AxiosError } from 'axios'

let meLoaded = false
let refreshFromStorageStarted = false

// 认证初始化状态：用于启动时的自动登录（access/refresh 恢复 + /auth/me 补全）
const authInitializing = ref(false)
const authReady = ref(false)
let bootstrapPromise: Promise<void> | null = null

function setCurrentUserProfile(store: ReturnType<typeof useAuthStore>, user: CurrentUser) {
  store.setUserProfile({
    userId: typeof user.userId === 'number' ? String(user.userId) : user.userId,
    username: user.username,
    email: user.email,
    avatar: user.avatar && user.avatar.trim() ? user.avatar : null,
    roleType: user.roleType,
    roles: user.roles,
  })
}

async function bootstrapAuth(store: ReturnType<typeof useAuthStore>) {
  if (bootstrapPromise) {
    return bootstrapPromise
  }

  authInitializing.value = true

  bootstrapPromise = (async () => {
    try {
      const cached = getTokens()

      if (!store.accessToken) {
        if (cached.accessToken) {
          // 本地已有 accessToken，直接恢复
          store.setTokens(cached.accessToken, cached.refreshToken ?? '')
        } else if (cached.refreshToken && !refreshFromStorageStarted) {
          // 只有 refreshToken，尝试用刷新接口获取一对新的令牌并自动登录
          refreshFromStorageStarted = true
          try {
            const res = await apiRefreshToken({ refreshToken: cached.refreshToken })
            store.setTokens(res.data.accessToken, res.data.refreshToken)
            setTokens(res.data.accessToken, res.data.refreshToken)
            const meRes = await getCurrentUser()
            setCurrentUserProfile(store, meRes.data)
            // 自动登录成功后启动定时器
            ensureTokenRefreshTimer()
          } catch (error) {
            const axiosError = error as AxiosError
            const status = axiosError.response?.status
            if (status === 401) {
              ElMessage.warning('登录验证已过期，请重新登录')
            }
            store.clear()
            clearTokens()
          }
        }
      }

      // 如果本地已有 accessToken：
      // 1. 没有用户信息，或
      // 2. 有用户信息但 avatar 为空（例如 login 返回的 avatar 为 null），
      // 则尝试用 /auth/me 补全最新的用户资料（含头像）
      if (store.accessToken && !meLoaded && (!store.user || !store.user.avatar)) {
        meLoaded = true
        try {
          const res = await getCurrentUser()
          setCurrentUserProfile(store, res.data)
        } catch {
          store.clear()
          clearTokens()
        }
      }
    } finally {
      authInitializing.value = false
      authReady.value = true
      // 初始化完成后，如果有token则启动定时器
      if (store.accessToken && store.refreshToken) {
        ensureTokenRefreshTimer()
      }
    }
  })()

  return bootstrapPromise
}

export function ensureAuthReady() {
  const store = useAuthStore()
  return bootstrapAuth(store)
}

export function useAuth() {
  const store = useAuthStore()

  // 启动时进行一次自动登录初始化（access/refresh 恢复 + /auth/me 补全）
  void ensureAuthReady()

  const isAuthed = computed(() => !!store.accessToken)

  const loginSuccess = (payload: LoginSuccessPayload) => {
    // 确保userId是字符串类型
    const data = {
      ...payload,
      userId: typeof payload.userId === 'number' ? String(payload.userId) : payload.userId,
    }
    store.setFromLogin(data)
    setTokens(data.accessToken, data.refreshToken)
    // 登录成功后启动定时器
    ensureTokenRefreshTimer()
  }

  const logout = async () => {
    try {
      await apiLogout()
    } catch {
      // 后端登出失败不阻塞前端本地退出
    } finally {
      // 登出时停止定时器
      stopTokenRefreshTimer()
      store.clear()
      clearTokens()
    }
  }

  return {
    accessToken: computed(() => store.accessToken),
    refreshToken: computed(() => store.refreshToken),
    user: computed(() => store.user),
    isAuthed,
    authInitializing: computed(() => authInitializing.value),
    authReady: computed(() => authReady.value),
    loginSuccess,
    logout,
  }
}
