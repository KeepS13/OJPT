import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      value: {
        username: 'demo-user',
        email: 'demo@example.com',
        avatar: null,
        roleType: 'USER',
      },
    },
  }),
}))

import UserCenterLayout from '@/components/layout/UserCenterLayout.vue'

describe('UserCenterLayout', () => {
  it('highlights only the submissions menu item on /profile/submissions', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/profile',
          component: UserCenterLayout,
          children: [
            { path: '', component: { template: '<div>profile</div>' } },
            { path: 'submissions', component: { template: '<div>submissions</div>' } },
            { path: 'security', component: { template: '<div>security</div>' } },
          ],
        },
      ],
    })

    router.push('/profile/submissions')
    await router.isReady()

    const wrapper = mount(UserCenterLayout, {
      global: {
        plugins: [router, ElementPlus],
        stubs: {
          UserAvatar: { template: '<div class="avatar-stub" />' },
        },
      },
    })

    const activeLabels = wrapper
      .findAll('.sidebar-nav-item.active .nav-label')
      .map((node) => node.text())

    expect(Array.from(new Set(activeLabels))).toEqual(['解题记录'])
  })
})
