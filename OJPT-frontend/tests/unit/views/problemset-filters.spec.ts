import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { defineComponent, h } from 'vue'

const { getProblemListMock, pushMock, routeMock } = vi.hoisted(() => ({
  getProblemListMock: vi.fn(),
  pushMock: vi.fn(),
  routeMock: { query: {} as Record<string, unknown> },
}))

vi.mock('@/api/problem', () => ({
  getProblemList: getProblemListMock,
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

import ProblemSetView from '@/views/ProblemSetView.vue'

const pagePayload = {
  records: [
    {
      id: '2100000000000000001',
      problemNo: 1,
      title: '两数之和',
      difficulty: 'EASY',
      acceptanceRate: 85.5,
      status: 'SOLVED',
      tags: [{ id: '3001', name: '数组', type: '基础' }],
    },
    {
      id: '2100000000000000002',
      problemNo: 2,
      title: '最长子串',
      difficulty: 'MEDIUM',
      acceptanceRate: 52.1,
      status: 'ATTEMPTED',
      tags: [{ id: '3002', name: '滑动窗口', type: '技巧' }],
    },
  ],
  total: 2,
  current: 1,
  size: 20,
  pages: 1,
}

const mountProblemSet = async () => {
  const wrapper = mount(ProblemSetView, {
    global: {
      plugins: [ElementPlus],
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a><slot /></a>',
        },
      },
    },
  })
  await flushPromises()
  return wrapper
}

describe('ProblemSetView filters', () => {
  beforeEach(() => {
    getProblemListMock.mockReset()
    pushMock.mockReset()
    routeMock.query = {}
    getProblemListMock.mockResolvedValue({ data: pagePayload })
    vi.useRealTimers()
  })

  it('initializes keyword from route query and sends it to the list api', async () => {
    routeMock.query = { keyword: 'P0001' }

    const wrapper = await mountProblemSet()

    expect((wrapper.get('[data-testid="problem-search-input"]').element as HTMLInputElement).value).toBe('P0001')
    expect(getProblemListMock).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      keyword: 'P0001',
    })

    wrapper.unmount()
  })

  it('filters by difficulty when a difficulty chip is clicked', async () => {
    const wrapper = await mountProblemSet()

    await wrapper.get('button.chip--easy').trigger('click')
    await flushPromises()

    expect(getProblemListMock).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      difficulty: 'EASY',
    })
  })

  it('filters by user progress status when a status chip is clicked', async () => {
    const wrapper = await mountProblemSet()
    const solvedButton = wrapper.findAll('button').find((button) => button.text() === '已通过')
    expect(solvedButton).toBeTruthy()

    await solvedButton!.trigger('click')
    await flushPromises()

    expect(getProblemListMock).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      status: 'SOLVED',
    })
  })

  it('filters by tag when a tag chip is clicked', async () => {
    const wrapper = await mountProblemSet()
    const tagButton = wrapper.findAll('button').find((button) => button.text() === '数组')
    expect(tagButton).toBeTruthy()

    await tagButton!.trigger('click')
    await flushPromises()

    expect(getProblemListMock).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      tagId: '3001',
    })
  })

  it('renders header stats from loaded problem data', async () => {
    const wrapper = await mountProblemSet()

    expect(wrapper.text()).toContain('/ 2')
    expect(wrapper.text()).toContain('1 / 2')
    expect(wrapper.text()).not.toContain('/ 4224')
  })

  it('does not render or send sorting controls', async () => {
    const wrapper = await mountProblemSet()

    expect(wrapper.text()).not.toContain('排序：')
    expect(getProblemListMock).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
    })
    expect(getProblemListMock.mock.lastCall?.[0]).not.toHaveProperty('orderBy')
  })

  it('clears keyword from the search clear button and refetches', async () => {
    vi.useFakeTimers()
    const wrapper = await mountProblemSet()

    await wrapper.get('[data-testid="problem-search-input"]').setValue('two sum')
    await vi.advanceTimersByTimeAsync(350)
    await flushPromises()
    expect(getProblemListMock).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      keyword: 'two sum',
    })

    await wrapper.get('[data-testid="problem-search-clear"]').trigger('click')
    await flushPromises()

    expect((wrapper.get('[data-testid="problem-search-input"]').element as HTMLInputElement).value).toBe('')
    expect(getProblemListMock).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
    })

    wrapper.unmount()
    vi.useRealTimers()
  })

  it('resets difficulty status tag and keyword from the clear filters button', async () => {
    vi.useFakeTimers()
    const wrapper = await mountProblemSet()

    await wrapper.get('button.chip--easy').trigger('click')
    await flushPromises()
    const solvedButton = wrapper.findAll('button').find((button) => button.text() === '已通过')
    await solvedButton!.trigger('click')
    await flushPromises()
    const tagButton = wrapper.findAll('button').find((button) => button.text() === '数组')
    await tagButton!.trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="problem-search-input"]').setValue('abc')
    await vi.advanceTimersByTimeAsync(350)
    await flushPromises()

    await wrapper.get('[data-testid="clear-filters-button"]').trigger('click')
    await flushPromises()

    expect(getProblemListMock).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
    })
    expect((wrapper.get('[data-testid="problem-search-input"]').element as HTMLInputElement).value).toBe('')

    wrapper.unmount()
    vi.useRealTimers()
  })

  it('renders a clearable empty state when no problem matches filters', async () => {
    getProblemListMock.mockResolvedValue({
      data: {
        ...pagePayload,
        records: [],
        total: 0,
      },
    })

    const wrapper = await mountProblemSet()

    expect(wrapper.text()).toContain('暂无匹配题目')
    expect(wrapper.find('[data-testid="empty-clear-filters"]').exists()).toBe(true)

    wrapper.unmount()
  })

  it('keeps problem title links and makes table rows navigable', async () => {
    const wrapper = await mountProblemSet()

    expect(wrapper.get('a.problem-link').attributes('href')).toBe('/problems/1')
    await wrapper.get('tr.problem-row').trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/problems/1')

    wrapper.unmount()
  })
})
