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

import { getProblemList, getProblemDetail, getProblemDetailByNo } from '../../../src/api/problem'

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

  it('getProblemDetailByNo 应该向 /problems/no/{problemNo} 发送 GET 请求', async () => {
    const problemNo = 1
    getMock.mockResolvedValue({ data: {} })

    await getProblemDetailByNo(problemNo)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith(`/problems/no/${problemNo}`)
  })

  describe('problemNo 字段验证', () => {
    it('getProblemList 应返回包含 problemNo 字段的数据', async () => {
      // 模拟后端统一响应格式：{ code: 200, data: PageResult<ProblemListItemVO> }
      const mockResponse = {
        code: 200,
        data: {
          records: [
            {
              id: '2100000000000000001',
              problemNo: 1,
              title: '两数之和',
              difficulty: 'EASY',
              acceptanceRate: 85.5,
              tags: []
            }
          ],
          total: 1,
          current: 1,
          size: 20,
          pages: 1
        },
        message: 'success',
        timestamp: Date.now()
      }
      getMock.mockResolvedValue(mockResponse)

      const res = await getProblemList({ page: 1, size: 20 })

      // getProblemList 返回的是 AxiosResponse，data 经过 extractApiData 提取后为 PageResult
      expect(res.data).toBeDefined()
      expect(res.data.records[0]).toHaveProperty('problemNo', 1)
      expect(res.data.records[0].problemNo).toBeGreaterThan(0)
      expect(typeof res.data.records[0].problemNo).toBe('number')
    })

    it('getProblemDetail 应返回包含 problemNo 字段的数据', async () => {
      const mockResponse = {
        code: 200,
        data: {
          id: '2100000000000000001',
          problemNo: 1,
          title: '两数之和',
          difficulty: 'EASY',
          statementMd: '# 题目描述',
          submitCount: 100,
          acceptedCount: 85,
          tags: []
        },
        message: 'success',
        timestamp: Date.now()
      }
      getMock.mockResolvedValue(mockResponse)

      const res = await getProblemDetailByNo(1)

      expect(res.data).toHaveProperty('problemNo', 1)
      expect(typeof res.data.problemNo).toBe('number')
    })
  })
})

