import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import UserAvatar from '@/components/common/UserAvatar.vue'

describe('UserAvatar', () => {
  it('uses same-origin avatar URLs for uploaded avatars', () => {
    const wrapper = mount(UserAvatar, {
      props: {
        name: 'demo',
        avatar: '/uploads/avatars/demo.webp',
      },
    })

    expect(wrapper.get('img').attributes('src')).toBe('/uploads/avatars/demo.webp')
  })
})
