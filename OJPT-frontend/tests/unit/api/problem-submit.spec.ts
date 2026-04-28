import { beforeEach, describe, expect, it, vi } from 'vitest'

const { postMock } = vi.hoisted(() => ({
  postMock: vi.fn(),
}))

vi.mock('@/api/request', () => ({
  default: {
    post: postMock,
  },
}))

import { submitProblemCode } from '../../../src/api/problem'

describe('problem submit api', () => {
  beforeEach(() => {
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
})
