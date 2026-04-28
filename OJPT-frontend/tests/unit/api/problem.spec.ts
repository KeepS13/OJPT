import { describe, it, expect, beforeEach, vi } from 'vitest'

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
  getProblemList,
  getProblemDetail,
  getProblemDetailByNo,
  getProblemSampleTestCases,
  runProblemCode,
  submitProblemCode,
} from '../../../src/api/problem'

describe('problem api', () => {
  beforeEach(() => {
    getMock.mockReset()
    postMock.mockReset()
  })

  it('getProblemList should send GET /problems with query params', async () => {
    const params = { page: 2, size: 20, keyword: 'two sum', difficulty: 'EASY' }
    getMock.mockResolvedValue({ data: {} })

    await getProblemList(params)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith('/problems', { params })
  })

  it('getProblemDetail should send GET /problems/{id}', async () => {
    const id = 123
    getMock.mockResolvedValue({ data: {} })

    await getProblemDetail(id)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith(`/problems/${id}`)
  })

  it('getProblemDetailByNo should send GET /problems/no/{problemNo}', async () => {
    const problemNo = 1
    getMock.mockResolvedValue({ data: {} })

    await getProblemDetailByNo(problemNo)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith(`/problems/no/${problemNo}`)
  })

  it('getProblemSampleTestCases should send GET /problems/no/{problemNo}/test-cases/sample', async () => {
    const problemNo = 1
    getMock.mockResolvedValue({ data: [] })

    await getProblemSampleTestCases(problemNo)

    expect(getMock).toHaveBeenCalledTimes(1)
    expect(getMock).toHaveBeenCalledWith(`/problems/no/${problemNo}/test-cases/sample`)
  })

  it('submitProblemCode should send POST /problems/no/{problemNo}/submissions', async () => {
    const problemNo = 1
    const payload = { language: 'Java', sourceCode: 'class Main {}' }
    postMock.mockResolvedValue({ data: {} })

    await submitProblemCode(problemNo, payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith(`/problems/no/${problemNo}/submissions`, payload)
  })

  it('runProblemCode should send POST /problems/run with code and cases', async () => {
    const payload = {
      language: 'Python3',
      sourceCode: 'print(input())',
      timeLimitMs: 1000,
      memoryLimitKb: 256000,
      cases: [{ inputText: 'abc', expectedOutput: 'abc' }],
    }
    postMock.mockResolvedValue({ data: {} })

    await runProblemCode(payload)

    expect(postMock).toHaveBeenCalledTimes(1)
    expect(postMock).toHaveBeenCalledWith('/problems/run', payload)
  })

  describe('problemNo field', () => {
    it('getProblemList should return records with problemNo', async () => {
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
              tags: [],
            },
          ],
          total: 1,
          current: 1,
          size: 20,
          pages: 1,
        },
        message: 'success',
        timestamp: Date.now(),
      }
      getMock.mockResolvedValue(mockResponse)

      const res = await getProblemList({ page: 1, size: 20 })

      expect(res.data).toBeDefined()
      expect(res.data.records[0]).toHaveProperty('problemNo', 1)
      expect(res.data.records[0].problemNo).toBeGreaterThan(0)
      expect(typeof res.data.records[0].problemNo).toBe('number')
    })

    it('getProblemDetailByNo should return a payload with problemNo', async () => {
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
          tags: [],
        },
        message: 'success',
        timestamp: Date.now(),
      }
      getMock.mockResolvedValue(mockResponse)

      const res = await getProblemDetailByNo(1)

      expect(res.data).toHaveProperty('problemNo', 1)
      expect(typeof res.data.problemNo).toBe('number')
    })
  })
})
