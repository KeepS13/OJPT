import { describe, it, expect, beforeEach, vi } from 'vitest'

// 使用 hoisted 工厂避免 vi.mock 提前提升导致的未初始化引用问题
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
  getSchoolList,
  updateSchoolStatus,
  getPlatformStatisticsOverview,
  getUserStatistics,
  getSchoolStatistics,
} from '../../../src/api/admin'

describe('admin api', () => {
  beforeEach(() => {
    getMock.mockReset()
    postMock.mockReset()
    putMock.mockReset()
    deleteMock.mockReset()
  })

  it('getUserList 应该向 /admin/users 发送 GET 请求并携带查询参数', async () => {
    const params = { page: 2, size: 20, status: 1 }
    getMock.mockResolvedValue({ data: {} })

    await getUserList(params)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith('/admin/users', { params })
  })

  it('updateUserStatus 应该向 /admin/users/{id}/status 发送 PUT 请求', async () => {
    putMock.mockResolvedValue({ data: {} })
    const userId = '123'
    const payload = { status: 0 }

    await updateUserStatus(userId, payload)

    expect(putMock).toHaveBeenCalledTimes(1)
    expect(putMock).toHaveBeenCalledWith(`/admin/users/${userId}/status`, payload)
  })

  it('getSchoolList 应该向 /admin/schools 发送 GET 请求并携带查询参数', async () => {
    const params = { page: 1, size: 10, status: 2 }
    getMock.mockResolvedValue({ data: {} })

    await getSchoolList(params)

    expect(getMock).toHaveBeenCalledWith('/admin/schools', { params })
  })

  it('updateSchoolStatus 应该向 /admin/schools/{id}/status 发送 PUT 请求', async () => {
    putMock.mockResolvedValue({ data: {} })
    const schoolId = '5'
    const payload = { status: 1 }

    await updateSchoolStatus(schoolId, payload)

    expect(putMock).toHaveBeenCalledWith(`/admin/schools/${schoolId}/status`, payload)
  })

  it('统计相关接口应调用 /admin/statistics/* 路径', async () => {
    getMock.mockResolvedValue({ data: {} })

    await getPlatformStatisticsOverview()
    await getUserStatistics()
    await getSchoolStatistics()

    expect(getMock).toHaveBeenCalledWith('/admin/statistics/overview')
    expect(getMock).toHaveBeenCalledWith('/admin/statistics/users')
    expect(getMock).toHaveBeenCalledWith('/admin/statistics/schools')
  })
})
