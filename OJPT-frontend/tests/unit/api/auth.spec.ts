import { describe, it, expect, vi, beforeEach } from 'vitest'

const { postMock, getMock } = vi.hoisted(() => ({
  postMock: vi.fn(),
  getMock: vi.fn(),
}))

vi.mock('@/api/request', () => ({
  default: {
    post: postMock,
    get: getMock,
  },
}))

import { login, register, refreshToken, logout, getCurrentUser } from '../../../src/api/auth'

describe('auth api', () => {
  beforeEach(() => {
    postMock.mockReset()
    getMock.mockReset()
  })

  it('login posts account and password to /auth/login', async () => {
    const payload = { account: 'user@example.com', password: '123456' }
    postMock.mockResolvedValue({ data: {} })

    await login(payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/auth/login', payload)
  })

  it('register posts registration payload to /auth/register', async () => {
    const payload = {
      account: 'new-user@example.com',
      password: '123456',
      nickname: '小明',
      gender: 1,
    }
    postMock.mockResolvedValue({ data: {} })

    await register(payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/auth/register', payload)
  })

  it('refreshToken posts refreshToken to /auth/refresh', async () => {
    const payload = { refreshToken: 'refresh-token-xxx' }
    postMock.mockResolvedValue({ data: {} })

    await refreshToken(payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/auth/refresh', payload)
  })

  it('logout posts to /auth/logout without body', async () => {
    postMock.mockResolvedValue({ data: {} })

    await logout()

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/auth/logout')
  })

  it('getCurrentUser gets /auth/me', async () => {
    getMock.mockResolvedValue({ data: {} })

    await getCurrentUser()

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith('/auth/me')
  })
})
