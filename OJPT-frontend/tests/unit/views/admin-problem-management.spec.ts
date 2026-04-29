import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

const {
  createAdminProblemMock,
  getAdminProblemListMock,
  publishAdminProblemMock,
  archiveAdminProblemMock,
  pushMock,
  successMessageMock,
  errorMessageMock,
} = vi.hoisted(() => ({
  createAdminProblemMock: vi.fn(),
  getAdminProblemListMock: vi.fn(),
  publishAdminProblemMock: vi.fn(),
  archiveAdminProblemMock: vi.fn(),
  pushMock: vi.fn(),
  successMessageMock: vi.fn(),
  errorMessageMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}))

vi.mock('@/api/admin', () => ({
  createAdminProblem: createAdminProblemMock,
  getAdminProblemList: getAdminProblemListMock,
  publishAdminProblem: publishAdminProblemMock,
  archiveAdminProblem: archiveAdminProblemMock,
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: successMessageMock,
      error: errorMessageMock,
    },
  }
})

import ProblemManagement from '@/views/admin/ProblemManagement.vue'

describe('ProblemManagement', () => {
  beforeEach(() => {
    createAdminProblemMock.mockReset()
    getAdminProblemListMock.mockReset()
    publishAdminProblemMock.mockReset()
    archiveAdminProblemMock.mockReset()
    pushMock.mockReset()
    successMessageMock.mockReset()
    errorMessageMock.mockReset()

    getAdminProblemListMock.mockResolvedValue({
      data: {
        records: [],
        total: 0,
      },
    })
    createAdminProblemMock.mockResolvedValue({
      data: {
        id: '2001',
      },
    })
  })

  it('creates a draft problem and opens it for editing', async () => {
    const wrapper = mount(ProblemManagement, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    await wrapper.get('[data-testid="create-problem-button"]').trigger('click')
    await flushPromises()

    expect(createAdminProblemMock).toHaveBeenCalledWith({
      title: expect.stringContaining('未命名题目'),
      difficulty: 'EASY',
      statementMd: expect.stringContaining('题目描述'),
      timeLimitMs: 1000,
      memoryLimitKb: 256000,
    })
    expect(pushMock).toHaveBeenCalledWith('/admin/problems/2001')

    wrapper.unmount()
  })
})
