import ElementPlus from 'element-plus'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TagManagement from '../TagManagement.vue'
import {
  createAdminTag,
  deleteAdminTag,
  getAdminTags,
  updateAdminTag,
} from '@/api/admin'

const { messageSuccess, messageError } = vi.hoisted(() => ({
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')

  return {
    ...actual,
    ElMessage: {
      success: messageSuccess,
      error: messageError,
    },
  }
})

vi.mock('@/api/admin', () => ({
  createAdminTag: vi.fn(),
  deleteAdminTag: vi.fn(),
  getAdminTags: vi.fn(),
  updateAdminTag: vi.fn(),
}))

describe('TagManagement', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn(() => true))
  })

  it('loads tags and creates a new tag', async () => {
    vi.mocked(getAdminTags)
      .mockResolvedValueOnce({
        data: [
          { id: '1', name: 'Array', type: 'DATA_STRUCTURE' },
          { id: '2', name: 'Graph', type: 'ALGORITHM' },
        ],
      } as never)
      .mockResolvedValueOnce({
        data: [
          { id: '1', name: 'Array', type: 'DATA_STRUCTURE' },
          { id: '2', name: 'Graph', type: 'ALGORITHM' },
          { id: '3', name: 'Greedy', type: 'ALGORITHM' },
        ],
      } as never)

    vi.mocked(createAdminTag).mockResolvedValue({
      data: { id: '3', name: 'Greedy', type: 'ALGORITHM' },
    } as never)

    const wrapper = mount(TagManagement, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          teleport: true,
          transition: false,
        },
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Array')
    expect(wrapper.text()).toContain('Graph')

    await wrapper.get('[data-testid="tag-name-input"] input').setValue('Greedy')
    await wrapper.get('[data-testid="tag-type-input"] input').setValue('ALGORITHM')
    await wrapper.get('[data-testid="tag-submit-button"]').trigger('click')

    await flushPromises()

    expect(createAdminTag).toHaveBeenCalledWith({
      name: 'Greedy',
      type: 'ALGORITHM',
    })
    expect(getAdminTags).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Greedy')
    expect(messageSuccess).toHaveBeenCalled()
  })

  it('edits an existing tag', async () => {
    vi.mocked(getAdminTags)
      .mockResolvedValueOnce({
        data: [
          { id: '1', name: 'Array', type: 'DATA_STRUCTURE' },
          { id: '2', name: 'Graph', type: 'ALGORITHM' },
        ],
      } as never)
      .mockResolvedValueOnce({
        data: [
          { id: '1', name: 'Array', type: 'DATA_STRUCTURE' },
          { id: '2', name: 'Shortest Path', type: 'ALGORITHM' },
        ],
      } as never)

    vi.mocked(updateAdminTag).mockResolvedValue({} as never)

    const wrapper = mount(TagManagement, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          teleport: true,
          transition: false,
        },
      },
    })

    await flushPromises()

    await wrapper.get('[data-testid="edit-tag-2"]').trigger('click')
    await wrapper.get('[data-testid="tag-name-input"] input').setValue('Shortest Path')
    await wrapper.get('[data-testid="tag-submit-button"]').trigger('click')

    await flushPromises()

    expect(updateAdminTag).toHaveBeenCalledWith('2', {
      name: 'Shortest Path',
      type: 'ALGORITHM',
    })
    expect(wrapper.text()).toContain('Shortest Path')
  })

  it('deletes a tag after confirmation', async () => {
    vi.mocked(getAdminTags)
      .mockResolvedValueOnce({
        data: [
          { id: '1', name: 'Array', type: 'DATA_STRUCTURE' },
          { id: '2', name: 'Graph', type: 'ALGORITHM' },
        ],
      } as never)
      .mockResolvedValueOnce({
        data: [{ id: '1', name: 'Array', type: 'DATA_STRUCTURE' }],
      } as never)

    vi.mocked(deleteAdminTag).mockResolvedValue({} as never)

    const wrapper = mount(TagManagement, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          teleport: true,
          transition: false,
        },
      },
    })

    await flushPromises()

    await wrapper.get('[data-testid="delete-tag-2"]').trigger('click')
    await flushPromises()

    expect(globalThis.confirm).toHaveBeenCalled()
    expect(deleteAdminTag).toHaveBeenCalledWith('2')
    expect(wrapper.text()).not.toContain('Graph')
    expect(messageSuccess).toHaveBeenCalled()
  })
})
