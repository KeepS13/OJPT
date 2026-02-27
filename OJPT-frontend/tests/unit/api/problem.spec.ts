import { describe, it, expect, beforeEach, vi } from 'vitest'

// 使用 hoisted 工厂避免 vi.mock 提前提升导致的未初始化引用问题
const { getMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
}))

vi.mock('@/api/request', () => ({
  default: {
    get: getMock,
  },
}))

import { getProblemList, getProblemDetail } from '../../../src/api/problem'

describe('problem api', () => {
  beforeEach(() => {
    getMock.mockReset()
  })

  it('getProblemList 应该向 /problems 发送 GET 请求并携带查询参数', async () => {
    const params = { page: 2, size: 20, keyword: 'two sum', difficulty: 'EASY' }
    getMock.mockResolvedValue({ data: {} })

    await getProblemList(params)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith('/problems', { params })
  })

  it('getProblemDetail 应该向 /problems/{id} 发送 GET 请求', async () => {
    const id = 123
    getMock.mockResolvedValue({ data: {} })

    await getProblemDetail(id)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith(`/problems/${id}`)
  })
})

