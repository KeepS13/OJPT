import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

const { getCurrentUserTrainingDashboardMock } = vi.hoisted(() => ({
  getCurrentUserTrainingDashboardMock: vi.fn(),
}))

vi.mock('@/api/user', async () => {
  return {
    getCurrentUserTrainingDashboard: getCurrentUserTrainingDashboardMock,
  }
})

import TrainingDashboardView from '@/views/TrainingDashboardView.vue'

describe('TrainingDashboardView', () => {
  beforeEach(() => {
    getCurrentUserTrainingDashboardMock.mockReset()
    getCurrentUserTrainingDashboardMock.mockResolvedValue({
      data: {
        totalSubmissions: 12,
        acceptedSubmissions: 9,
        solvedProblemCount: 5,
        acceptanceRate: 75,
        recentSubmissions: [
          {
            submissionId: '9001',
            problemId: '2001',
            problemNo: 1,
            problemTitle: '两数之和',
            language: 'Java',
            status: 'AC',
            timeMs: 12,
            memoryKb: 128,
            createdAt: '2026-04-29T10:00:00',
          },
        ],
        statusDistribution: {
          AC: 9,
          WA: 2,
          CE: 1,
        },
        difficultyDistribution: {
          EASY: 3,
          MEDIUM: 2,
        },
      },
    })
  })

  it('loads and renders the current user training dashboard', async () => {
    const wrapper = mount(TrainingDashboardView, {
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    expect(getCurrentUserTrainingDashboardMock).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('训练看板')
    expect(wrapper.text()).toContain('12')
    expect(wrapper.text()).toContain('9')
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('75%')
    expect(wrapper.text()).toContain('状态分布')
    expect(wrapper.text()).toContain('难度分布')
    expect(wrapper.text()).toContain('两数之和')
    expect(wrapper.text()).toContain('AC')
  })
})
