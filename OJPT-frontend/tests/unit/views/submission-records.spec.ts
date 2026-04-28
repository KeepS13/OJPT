import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

const { getCurrentUserSubmissionRecordsMock } = vi.hoisted(() => ({
  getCurrentUserSubmissionRecordsMock: vi.fn(),
}))

vi.mock('@/api/user', async (importOriginal) => {
  return {
    getCurrentUserSubmissionRecords: getCurrentUserSubmissionRecordsMock,
  }
})

import SubmissionRecordsView from '@/views/SubmissionRecordsView.vue'

describe('SubmissionRecordsView', () => {
  beforeEach(() => {
    getCurrentUserSubmissionRecordsMock.mockReset()
    getCurrentUserSubmissionRecordsMock.mockResolvedValue({
      data: {
        records: [
          {
            submissionId: '9001',
            problemId: '2001',
            problemNo: 1,
            problemTitle: '两数之和',
            language: 'Java',
            status: 'AC',
            sourceCode: 'public class Main {}',
            timeMs: 12,
            memoryKb: 256,
            createdAt: '2026-04-27T18:00:00',
          },
        ],
        total: 1,
        current: 1,
        size: 10,
        pages: 1,
      },
    })
  })

  it('loads and renders user submission history', async () => {
    const wrapper = mount(SubmissionRecordsView, {
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    expect(getCurrentUserSubmissionRecordsMock).toHaveBeenCalledWith({ page: 1, size: 10 })
    expect(wrapper.text()).toContain('两数之和')
    expect(wrapper.text()).toContain('Java')
    expect(wrapper.text()).toContain('AC')
    expect(wrapper.text()).toContain('查看代码')
  })
})
