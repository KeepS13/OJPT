import { describe, it, expect, vi, beforeEach } from 'vitest'

// 使用 hoisted 工厂避免 vi.mock 提前提升导致的未初始化引用问题
const { postMock, getMock } = vi.hoisted(() => ({
  postMock: vi.fn(),
  getMock: vi.fn(),
}))

// 使用别名路径 mock 掉真实的 axios 实例，避免触发 request.ts 中复杂的拦截器与 Pinia 依赖
vi.mock('@/api/request', () => ({
  default: {
    post: postMock,
    get: getMock,
  },
}))

import { login, refreshToken, logout, getCurrentUser } from '../../../src/api/auth'

describe('auth api', () => {
  beforeEach(() => {
    postMock.mockReset()
    getMock.mockReset()
  })

  it('login 应该向 /auth/login 发送账号与密码', async () => {
    const payload = { account: 'user@example.com', password: '123456' }
    postMock.mockResolvedValue({ data: {} })

    await login(payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/auth/login', payload)
  })

  it('refreshToken 应该向 /auth/refresh 发送 refreshToken', async () => {
    const payload = { refreshToken: 'refresh-token-xxx' }
    postMock.mockResolvedValue({ data: {} })

    await refreshToken(payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/auth/refresh', payload)
  })

  it('logout 应该向 /auth/logout 发送 POST 请求且不带请求体', async () => {
    postMock.mockResolvedValue({ data: {} })

    await logout()

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/auth/logout')
  })

  it('getCurrentUser 应该向 /auth/me 发起 GET 请求', async () => {
    getMock.mockResolvedValue({ data: {} })

    await getCurrentUser()

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith('/auth/me')
  })
})

