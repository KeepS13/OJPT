import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import OverviewTab from '../OverviewTab.vue'
import {
  getJudgeEnvironmentHealth,
  getPlatformStatisticsOverview,
  getUserStatistics,
} from '@/api/admin'

const { messageError } = vi.hoisted(() => ({
  messageError: vi.fn(),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')

  return {
    ...actual,
    ElMessage: {
      error: messageError,
    },
  }
})

vi.mock('@/api/admin', () => ({
  getJudgeEnvironmentHealth: vi.fn(),
  getPlatformStatisticsOverview: vi.fn(),
  getUserStatistics: vi.fn(),
}))

describe('OverviewTab', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    vi.mocked(getPlatformStatisticsOverview).mockResolvedValue({
      data: {
        totalCount: 18,
        statusCount: {
          users: 18,
        },
      },
    } as never)

    vi.mocked(getUserStatistics).mockResolvedValue({
      data: {
        totalCount: 18,
        statusCount: {
          0: 1,
          1: 15,
          2: 2,
        },
      },
    } as never)

    vi.mocked(getJudgeEnvironmentHealth).mockResolvedValue({
      data: {
        status: 'DOWN',
        message: 'Some judge Docker environment checks failed',
        checks: [
          {
            name: 'docker-executable',
            status: 'UP',
            target: 'C:/Docker/docker.exe',
            message: 'Docker executable exists',
          },
          {
            name: 'docker-version',
            status: 'UP',
            target: 'C:/Docker/docker.exe',
            message: 'Command completed successfully',
          },
          {
            name: 'docker-info',
            status: 'UP',
            target: 'C:/Docker/docker.exe',
            message: 'Command completed successfully',
          },
          {
            name: 'image-cpp',
            status: 'UP',
            target: 'cpp',
            message: 'Image gcc:13.2.0 is available',
          },
          {
            name: 'image-java',
            status: 'DOWN',
            target: 'java',
            message: 'Command failed with exit code 1: No such image',
          },
          {
            name: 'image-python',
            status: 'UP',
            target: 'python',
            message: 'Image python:3.11 is available',
          },
        ],
      },
    } as never)
  })

  it('loads and renders judge environment health details', async () => {
    const wrapper = mount(OverviewTab)

    await flushPromises()

    expect(getPlatformStatisticsOverview).toHaveBeenCalledTimes(1)
    expect(getUserStatistics).toHaveBeenCalledTimes(1)
    expect(getJudgeEnvironmentHealth).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('判题环境')
    expect(wrapper.text()).toContain('判题 Docker 环境存在异常')
    expect(wrapper.text()).toContain('C:/Docker/docker.exe')
    expect(wrapper.text()).toContain('No such image')
    expect(wrapper.text()).toContain('异常')
  })
})
