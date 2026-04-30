import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import SecurityView from '@/views/SecurityView.vue'

const {
  deleteAccountMock,
  getCurrentUserDetailMock,
  clearTokensMock,
  stopTokenRefreshTimerMock,
  routerPushMock,
} = vi.hoisted(() => ({
  deleteAccountMock: vi.fn(),
  getCurrentUserDetailMock: vi.fn(),
  clearTokensMock: vi.fn(),
  stopTokenRefreshTimerMock: vi.fn(),
  routerPushMock: vi.fn(),
}))

vi.mock('@/api/user', () => ({
  getCurrentUserDetail: getCurrentUserDetailMock,
  updateUsername: vi.fn(),
  updateEmail: vi.fn(),
  updatePhone: vi.fn(),
  updatePassword: vi.fn(),
  deleteAccount: deleteAccountMock,
}))

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      value: {
        username: 'demo-user',
        email: 'demo@example.com',
      },
    },
  }),
}))

vi.mock('@/utils/storage', () => ({
  clearTokens: clearTokensMock,
}))

vi.mock('@/api/request', () => ({
  stopTokenRefreshTimer: stopTokenRefreshTimerMock,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPushMock,
  }),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
  },
  ElDialog: {
    props: ['modelValue'],
    template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>',
  },
}))

const elementStubs = {
  ElDialog: {
    props: ['modelValue'],
    template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>',
  },
  ElButton: {
    emits: ['click'],
    template: '<button :class="$attrs.class" :type="$attrs.type" @click="$emit(\'click\')"><slot /></button>',
  },
  ElForm: {
    template: '<form><slot /></form>',
  },
  ElFormItem: {
    template: '<div><slot /></div>',
  },
  ElInput: {
    template: '<input />',
  },
}

describe('SecurityView account deletion', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    deleteAccountMock.mockResolvedValue({ data: undefined })
    getCurrentUserDetailMock.mockResolvedValue({
      data: {
        username: 'demo-user',
        email: 'demo@example.com',
        phone: '13800000000',
      },
    })
    clearTokensMock.mockReset()
    stopTokenRefreshTimerMock.mockReset()
    routerPushMock.mockReset()
  })

  it('clears persisted auth state after deleting the account', async () => {
    const wrapper = mount(SecurityView, {
      global: {
        stubs: elementStubs,
      },
    })
    await flushPromises()

    await wrapper.get('.delete-btn').trigger('click')
    await nextTick()
    await wrapper.get('button.el-button--danger').trigger('click')
    await nextTick()
    await wrapper.get('button.el-button--danger').trigger('click')
    await flushPromises()

    expect(deleteAccountMock).toHaveBeenCalledTimes(1)
    expect(stopTokenRefreshTimerMock).toHaveBeenCalledTimes(1)
    expect(clearTokensMock).toHaveBeenCalledTimes(1)
    expect(routerPushMock).toHaveBeenCalledWith('/')
  })
})
