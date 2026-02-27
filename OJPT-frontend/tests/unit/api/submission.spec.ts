import { describe, it, expect, beforeEach, vi } from 'vitest'

// 使用 hoisted 工厂避免 vi.mock 提前提升导致的未初始化引用问题
const { getMock, postMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
}))

vi.mock('@/api/request', () => ({
  default: {
    get: getMock,
    post: postMock,
  },
}))

import {
  createSubmission,
  getSubmission,
  getMySubmissions,
  adminUpdateSubmissionStatus,
} from '../../../src/api/submission'

describe('submission api', () => {
  beforeEach(() => {
    getMock.mockReset()
    postMock.mockReset()
  })

  it('createSubmission 应该向 /submissions 发送 POST 请求', async () => {
    const payload = { problemId: 1, language: 'C++', sourceCode: 'code' }
    postMock.mockResolvedValue({ data: {} })

    await createSubmission(payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/submissions', payload)
  })

  it('getSubmission 应该向 /submissions/{id} 发送 GET 请求', async () => {
    const id = 100
    getMock.mockResolvedValue({ data: {} })

    await getSubmission(id)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith(`/submissions/${id}`)
  })

  it('getMySubmissions 应该向 /submissions 发送 GET 请求并携带查询参数', async () => {
    const params = { problemId: 1, page: 1, size: 10 }
    getMock.mockResolvedValue({ data: {} })

    await getMySubmissions(params)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith('/submissions', { params })
  })

  it('adminUpdateSubmissionStatus 应该向 /admin/submissions/{id}:setStatus 发送 POST 请求', async () => {
    const id = 99
    const payload = { status: 'AC', timeMs: 10 }
    postMock.mockResolvedValue({ data: {} })

    await adminUpdateSubmissionStatus(id, payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith(`/admin/submissions/${id}:setStatus`, payload)
  })
})

