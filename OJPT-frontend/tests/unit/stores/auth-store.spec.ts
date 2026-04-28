import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../../../src/stores/auth'

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('setTokens stores access and refresh tokens', () => {
    const store = useAuthStore()

    store.setTokens('access-1', 'refresh-1')

    expect(store.accessToken).toBe('access-1')
    expect(store.refreshToken).toBe('refresh-1')
  })

  it('setFromLogin normalizes admin payloads to the admin role only', () => {
    const store = useAuthStore()

    store.setFromLogin({
      accessToken: 'access-2',
      refreshToken: 'refresh-2',
      userId: 1234567890123456,
      username: 'test-user',
      email: 'test@example.com',
      avatar: '  ',
      roleType: 'ADMIN',
      roles: ['ADMIN', 'USER'],
    })

    expect(store.accessToken).toBe('access-2')
    expect(store.refreshToken).toBe('refresh-2')
    expect(store.user).not.toBeNull()
    expect(store.user?.username).toBe('test-user')
    expect(store.user?.email).toBe('test@example.com')
    expect(store.user?.avatar).toBeNull()
    expect(store.user?.userId).toBe(String(1234567890123456))
    expect(store.user?.roleType).toBe('ADMIN')
    expect(store.user?.roles).toEqual(['ADMIN'])
  })

  it('setFromLogin collapses legacy non-admin roles to the user role', () => {
    const store = useAuthStore()

    store.setFromLogin({
      accessToken: 'access-legacy',
      refreshToken: 'refresh-legacy',
      userId: 'legacy-1',
      username: 'legacy-user',
      email: 'legacy@example.com',
      avatar: null,
      roleType: 'TEACHER',
      roles: ['USER', 'TEACHER', 'SCHOOL'],
    })

    expect(store.user?.roleType).toBe('USER')
    expect(store.user?.roles).toEqual(['USER'])
  })

  it('clear removes tokens and user state', () => {
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
