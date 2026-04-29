import { beforeEach, describe, expect, it, vi } from 'vitest'

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

import { getProblemSubmissionResult, submitProblemCode } from '../../../src/api/problem'

describe('problem submit api', () => {
  beforeEach(() => {
    getMock.mockReset()
    postMock.mockReset()
  })

  it('posts code submission to /problems/no/{problemNo}/submissions', async () => {
    postMock.mockResolvedValue({ data: {} })

    await submitProblemCode(1, {
      language: 'Java',
      sourceCode: 'public class Main {}',
    })

    expect(postMock).toHaveBeenCalledWith(
      '/problems/no/1/submissions',
      {
        language: 'Java',
        sourceCode: 'public class Main {}',
      },
      {
        timeout: 60000,
      },
    )
  })

  it('gets submission result from /problems/submissions/{submissionId}', async () => {
    getMock.mockResolvedValue({ data: {} })

    await getProblemSubmissionResult(9001)

    expect(getMock).toHaveBeenCalledWith('/problems/submissions/9001')
  })
})
