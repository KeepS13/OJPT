import { describe, it, expect, beforeEach, vi } from 'vitest'

const { getMock, postMock, putMock, deleteMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
  putMock: vi.fn(),
  deleteMock: vi.fn(),
}))

vi.mock('@/api/request', () => ({
  default: {
    get: getMock,
    post: postMock,
    put: putMock,
    delete: deleteMock,
  },
}))

import {
  getUserList,
  updateUserStatus,
  getPlatformStatisticsOverview,
  getUserStatistics,
  createAdminProblem,
  getAdminProblemTestCases,
  replaceAdminProblemTestCases,
} from '../../../src/api/admin'

describe('admin api', () => {
  beforeEach(() => {
    getMock.mockReset()
    postMock.mockReset()
    putMock.mockReset()
    deleteMock.mockReset()
  })

  it('getUserList should request GET /admin/users with params', async () => {
    const params = { page: 2, size: 20, status: 1 }
    getMock.mockResolvedValue({ data: {} })

    await getUserList(params)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith('/admin/users', { params })
  })

  it('updateUserStatus should request PUT /admin/users/{id}/status', async () => {
    putMock.mockResolvedValue({ data: {} })
    const userId = '123'
    const payload = { status: 0 }

    await updateUserStatus(userId, payload)

    expect(putMock).toHaveBeenCalledTimes(1)
    expect(putMock).toHaveBeenCalledWith(`/admin/users/${userId}/status`, payload)
  })

  it('statistics endpoints should request /admin/statistics/*', async () => {
    getMock.mockResolvedValue({ data: {} })

    await getPlatformStatisticsOverview()
    await getUserStatistics()

    expect(getMock).toHaveBeenCalledWith('/admin/statistics/overview')
    expect(getMock).toHaveBeenCalledWith('/admin/statistics/users')
  })

  it('createAdminProblem should request POST /admin/problems for a draft', async () => {
    postMock.mockResolvedValue({ data: { id: '1001' } })
    const payload = {
      title: 'New draft',
      difficulty: 'EASY' as const,
      statementMd: '## Statement',
      timeLimitMs: 1000,
      memoryLimitKb: 256000,
    }

    await createAdminProblem(payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/admin/problems', payload)
  })

  it('getAdminProblemTestCases should request GET /admin/problems/{id}/test-cases', async () => {
    getMock.mockResolvedValue({ data: [] })

    await getAdminProblemTestCases('1001')

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith('/admin/problems/1001/test-cases')
  })

  it('replaceAdminProblemTestCases should request PUT /admin/problems/{id}/test-cases', async () => {
    putMock.mockResolvedValue({ data: undefined })
    const payload = {
      cases: [
        {
          id: '1',
          caseType: 'SAMPLE',
          sortOrder: 1,
          inputText: '1 2',
          expectedOutput: '3',
          explanation: 'basic sample',
        },
      ],
    }

    await replaceAdminProblemTestCases('1001', payload)

    expect(putMock).toHaveBeenCalledTimes(1)
    expect(putMock).toHaveBeenCalledWith('/admin/problems/1001/test-cases', payload)
  })
})
