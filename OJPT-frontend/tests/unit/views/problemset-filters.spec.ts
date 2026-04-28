import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

const { getProblemListMock } = vi.hoisted(() => ({
  getProblemListMock: vi.fn(),
}))

vi.mock('@/api/problem', () => ({
  getProblemList: getProblemListMock,
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
    getProblemListMock.mockResolvedValue({ data: pagePayload })
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
})
