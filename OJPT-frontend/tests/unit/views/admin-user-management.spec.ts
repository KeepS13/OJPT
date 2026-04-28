import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

const {
  getUserListMock,
  getUserDetailMock,
  updateUserMock,
  deleteUserMock,
  updateUserStatusMock,
  successMessageMock,
  errorMessageMock,
} = vi.hoisted(() => ({
  getUserListMock: vi.fn(),
  getUserDetailMock: vi.fn(),
  updateUserMock: vi.fn(),
  deleteUserMock: vi.fn(),
  updateUserStatusMock: vi.fn(),
  successMessageMock: vi.fn(),
  errorMessageMock: vi.fn(),
}))

vi.mock('@/api/admin', () => ({
  getUserList: getUserListMock,
  getUserDetail: getUserDetailMock,
  updateUser: updateUserMock,
  deleteUser: deleteUserMock,
  updateUserStatus: updateUserStatusMock,
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: successMessageMock,
      error: errorMessageMock,
    },
  }
})

import UserManagement from '@/views/admin/UserManagement.vue'

const listUser = {
  userId: '1',
  username: 'demo-user',
  email: 'demo@example.com',
  phone: '13800000000',
  avatar: null,
  roleType: 'USER',
  status: 1,
  roles: ['USER'],
  createdAt: '2026-04-27T10:00:00',
  updatedAt: '2026-04-27T10:00:00',
}

const detailedUser = {
  ...listUser,
  gender: 1,
  birthday: '2000-01-02',
  address: 'Shanghai',
  website: 'https://example.com',
  github: 'octocat',
  company: 'OpenAI',
  position: 'Engineer',
  skills: 'Vue, TypeScript',
  studentNo: 'S2026001',
  schoolId: '1001',
  bio: 'Loves algorithms',
  tags: 'frontend,oj',
}

describe('UserManagement', () => {
  const mountUserManagement = () =>
    mount(UserManagement, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

  beforeEach(() => {
    getUserListMock.mockReset()
    getUserDetailMock.mockReset()
    updateUserMock.mockReset()
    deleteUserMock.mockReset()
    updateUserStatusMock.mockReset()
    successMessageMock.mockReset()
    errorMessageMock.mockReset()

    getUserListMock.mockResolvedValue({
      data: {
        records: [listUser],
        total: 1,
      },
    })
    getUserDetailMock.mockResolvedValue({ data: detailedUser })
    updateUserMock.mockResolvedValue({ data: undefined })
    deleteUserMock.mockResolvedValue({ data: undefined })
    updateUserStatusMock.mockResolvedValue({ data: undefined })
  })

  it('exposes full profile fields in the admin edit dialog', async () => {
    const wrapper = mountUserManagement()

    await flushPromises()

    await wrapper.get('[data-testid="edit-user-1"]').trigger('click')
    await flushPromises()

    expect(getUserDetailMock).toHaveBeenCalledWith('1')
    expect(document.body.textContent).toContain('地址')
    expect(document.body.textContent).toContain('公司')
    expect(document.body.textContent).toContain('简介')
    expect(document.body.querySelector('[data-testid="user-form-bio"] textarea')).toHaveProperty(
      'value',
      'Loves algorithms',
    )

    wrapper.unmount()
  })

  it('supports setting a user status to pending review', async () => {
    const wrapper = mountUserManagement()

    await flushPromises()

    await wrapper.get('[data-testid="set-status-pending-1"]').trigger('click')
    await flushPromises()

    expect(updateUserStatusMock).toHaveBeenCalledWith('1', { status: 2 })

    wrapper.unmount()
  })

  it('shows a detail dialog for the selected user', async () => {
    const wrapper = mountUserManagement()

    await flushPromises()

    await wrapper.get('[data-testid="view-user-1"]').trigger('click')
    await flushPromises()

    expect(getUserDetailMock).toHaveBeenCalledWith('1')
    expect(document.body.textContent).toContain('GitHub')
    expect(document.body.textContent).toContain('octocat')
    expect(document.body.textContent).toContain('创建时间')

    wrapper.unmount()
  })
})
