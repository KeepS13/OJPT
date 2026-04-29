import { mount } from '@vue/test-utils'
import { defineComponent, h, inject, provide } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminLayout from '../AdminLayout.vue'

const { push, route } = vi.hoisted(() => ({
  push: vi.fn(),
  route: { path: '/admin' },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push,
  }),
  useRoute: () => route,
}))

vi.mock('@element-plus/icons-vue', () => ({
  Collection: defineComponent({
    name: 'CollectionIcon',
    setup() {
      return () => h('span', 'collection')
    },
  }),
  DataAnalysis: defineComponent({
    name: 'DataAnalysisIcon',
    setup() {
      return () => h('span', 'analysis')
    },
  }),
  PriceTag: defineComponent({
    name: 'PriceTagIcon',
    setup() {
      return () => h('span', 'tag')
    },
  }),
  User: defineComponent({
    name: 'UserIcon',
    setup() {
      return () => h('span', 'user')
    },
  }),
}))

vi.mock('element-plus', () => {
  const ElContainer = defineComponent({
    name: 'ElContainer',
    setup(_, { slots }) {
      return () => h('div', slots.default?.())
    },
  })

  const ElAside = defineComponent({
    name: 'ElAside',
    setup(_, { slots }) {
      return () => h('aside', slots.default?.())
    },
  })

  const ElMain = defineComponent({
    name: 'ElMain',
    setup(_, { slots }) {
      return () => h('main', slots.default?.())
    },
  })

  const ElHeader = defineComponent({
    name: 'ElHeader',
    setup(_, { slots }) {
      return () => h('header', slots.default?.())
    },
  })

  const ElMenu = defineComponent({
    name: 'ElMenu',
    emits: ['select'],
    setup(_, { slots, emit }) {
      provide('onAdminMenuSelect', (path: string) => emit('select', path))
      return () => h('nav', slots.default?.())
    },
  })

  const ElMenuItem = defineComponent({
    name: 'ElMenuItem',
    props: {
      index: {
        type: String,
        required: true,
      },
    },
    setup(props, { slots }) {
      const onSelect = inject<(path: string) => void>('onAdminMenuSelect')
      return () =>
        h(
          'button',
          {
            type: 'button',
            'data-index': props.index,
            onClick: () => onSelect?.(props.index),
          },
          slots.default?.(),
        )
    },
  })

  return {
    ElAside,
    ElContainer,
    ElHeader,
    ElMain,
    ElMenu,
    ElMenuItem,
  }
})

describe('AdminLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    route.path = '/admin'
  })

  it('adds a tag management entry to the admin menu', async () => {
    const wrapper = mount(AdminLayout, {
      global: {
        stubs: {
          RouterView: true,
          ElIcon: defineComponent({
            name: 'ElIcon',
            setup(_, { slots }) {
              return () => h('span', slots.default?.())
            },
          }),
        },
      },
    })

    expect(wrapper.text()).toContain('标签管理')

    await wrapper.get('[data-index="/admin/tags"]').trigger('click')

    expect(push).toHaveBeenCalledWith('/admin/tags')
  })
})
