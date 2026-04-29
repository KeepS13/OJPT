import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'

const { pushMock, routeMock, logoutMock } = vi.hoisted(() => ({
  pushMock: vi.fn(),
  routeMock: { query: {} as Record<string, unknown> },
  logoutMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
  useRoute: () => routeMock,
  RouterLink: defineComponent({
    props: {
      to: {
        type: [String, Object],
        default: '',
      },
    },
    setup(props, { slots }) {
      return () => h('a', { href: String(props.to) }, slots.default?.())
    },
  }),
}))

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    isAuthed: { value: false },
    user: { value: null },
    logout: logoutMock,
  }),
}))

vi.mock('@/components/auth/LoginDialog.vue', () => ({
  default: defineComponent({
    props: {
      modelValue: {
        type: Boolean,
        default: false,
      },
    },
    emits: ['update:modelValue'],
    template: '<div />',
  }),
}))

vi.mock('@/components/common/UserAvatar.vue', () => ({
  default: defineComponent({
    template: '<div />',
  }),
}))

import TopNav from '@/components/layout/TopNav.vue'

describe('TopNav search', () => {
  beforeEach(() => {
    pushMock.mockReset()
    logoutMock.mockReset()
    routeMock.query = {}
  })

  it('navigates to problemset keyword query on enter', async () => {
    const wrapper = mount(TopNav, {
      global: {
        stubs: {
          LoginDialog: { template: '<div />' },
          UserAvatar: { template: '<div />' },
        },
      },
    })

    await wrapper.get('[data-testid="topnav-search-input"]').setValue(' P0001 ')
    await wrapper.get('[data-testid="topnav-search-input"]').trigger('keydown.enter')

    expect(pushMock).toHaveBeenCalledWith({
      path: '/problemset',
      query: { keyword: 'P0001' },
    })
  })
})
