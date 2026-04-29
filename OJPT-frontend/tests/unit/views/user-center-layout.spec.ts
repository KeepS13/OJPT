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
            { path: 'training', component: { template: '<div>training</div>' } },
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

    const activeItem = wrapper.find('.sidebar-nav-item.active')
    expect(activeItem.attributes('href')).toBe('/profile/submissions')
  })

  it('renders and highlights the training dashboard entry on /profile/training', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/profile',
          component: UserCenterLayout,
          children: [
            { path: '', component: { template: '<div>profile</div>' } },
            { path: 'training', component: { template: '<div>training</div>' } },
            { path: 'submissions', component: { template: '<div>submissions</div>' } },
            { path: 'security', component: { template: '<div>security</div>' } },
          ],
        },
      ],
    })

    router.push('/profile/training')
    await router.isReady()

    const wrapper = mount(UserCenterLayout, {
      global: {
        plugins: [router, ElementPlus],
        stubs: {
          UserAvatar: { template: '<div class="avatar-stub" />' },
        },
      },
    })

    const links = wrapper.findAll('.sidebar-nav-item').map((node) => node.attributes('href'))
    expect(links).toContain('/profile/training')

    const activeItem = wrapper.find('.sidebar-nav-item.active')
    expect(activeItem.attributes('href')).toBe('/profile/training')
  })
})
