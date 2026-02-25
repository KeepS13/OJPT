// 简单的 localStorage 封装，后续如果需要可以扩展为带命名空间、过期时间等

const ACCESS_TOKEN_KEY = 'ojpt_access_token'
const REFRESH_TOKEN_KEY = 'ojpt_refresh_token'

export function setTokens(accessToken: string, refreshToken: string) {
  // 方案B（混合存储）：
  // - accessToken 放 sessionStorage（降低 XSS 暴露窗口）
  // - refreshToken 放 localStorage（支持持久化登录/自动刷新）
  sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearTokens() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function getTokens() {
  const accessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY)
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  return {
    accessToken,
    refreshToken,
  }
}


