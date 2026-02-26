import { beforeEach, describe, expect, it } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../../../src/stores/auth'

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('setTokens 应正确设置 accessToken 与 refreshToken', () => {
    const store = useAuthStore()

    store.setTokens('access-1', 'refresh-1')

    expect(store.accessToken).toBe('access-1')
    expect(store.refreshToken).toBe('refresh-1')
  })

  it('setFromLogin 会根据登录响应填充 token 与用户信息，并将空头像视为 null', () => {
    const store = useAuthStore()

    store.setFromLogin({
      accessToken: 'access-2',
      refreshToken: 'refresh-2',
      userId: 1234567890123456,
      username: 'test-user',
      email: 'test@example.com',
      avatar: '  ', // 空白头像应被视为 null
      roleType: 'ADMIN',
      roles: ['ADMIN'],
    })

    expect(store.accessToken).toBe('access-2')
    expect(store.refreshToken).toBe('refresh-2')

    expect(store.user).not.toBeNull()
    expect(store.user?.username).toBe('test-user')
    expect(store.user?.email).toBe('test@example.com')
    expect(store.user?.avatar).toBeNull()
    // userId 应被强制转换为字符串以避免大整数精度问题
    expect(store.user?.userId).toBe(String(1234567890123456))
    expect(store.user?.roles).toEqual(['ADMIN'])
  })

  it('clear 应清空 token 与用户信息', () => {
    const store = useAuthStore()
    store.setTokens('access-x', 'refresh-x')
    store.user = {
      userId: '1',
      username: 'user',
      email: 'user@example.com',
      avatar: null,
      roleType: 'USER',
      roles: ['USER'],
    }

    store.clear()

    expect(store.accessToken).toBe('')
    expect(store.refreshToken).toBe('')
    expect(store.user).toBeNull()
  })
})

