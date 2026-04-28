import { describe, it, expect, beforeEach, vi } from 'vitest'

const { getMock, postMock, putMock, deleteMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
  putMock: vi.fn(),
  deleteMock: vi.fn(),
}))

vi.mock('@/api/request', () => ({
  default: {
    get: getMock,
    post: postMock,
    put: putMock,
    delete: deleteMock,
  },
}))

import {
  getCurrentUserDetail,
  getCurrentUserSubmissionRecords,
  updateUserInfo,
  uploadAvatar,
  deleteAvatar,
  updateUsername,
  updateEmail,
  updatePhone,
  updatePassword,
  deleteAccount,
} from '../../../src/api/user'

describe('user api', () => {
  beforeEach(() => {
    getMock.mockReset()
    postMock.mockReset()
    putMock.mockReset()
    deleteMock.mockReset()
  })

  it('getCurrentUserDetail calls /users/me/detail', async () => {
    getMock.mockResolvedValue({ data: {} })

    await getCurrentUserDetail()

    expect(getMock).toHaveBeenCalledWith('/users/me/detail')
  })

  it('getCurrentUserSubmissionRecords calls /users/me/submissions with paging params', async () => {
    getMock.mockResolvedValue({ data: {} })

    await getCurrentUserSubmissionRecords({ page: 2, size: 20 })

    expect(getMock).toHaveBeenCalledWith('/users/me/submissions', {
      params: { page: 2, size: 20 },
    })
  })

  it('updateUserInfo uses PUT /users/me', async () => {
    const payload = { email: 'test@example.com' }
    putMock.mockResolvedValue({ data: {} })

    await updateUserInfo(payload)

    expect(putMock).toHaveBeenCalledWith('/users/me', payload)
  })

  it('uploadAvatar uses multipart/form-data on /users/me/avatar', async () => {
    const file = new File(['content'], 'avatar.webp', { type: 'image/webp' })
    postMock.mockResolvedValue({ data: {} })

    await uploadAvatar(file)

    expect(postMock).toHaveBeenCalledTimes(1)
    const [url, formData, config] = postMock.mock.calls[0]
    expect(url).toBe('/users/me/avatar')
    expect(formData).toBeInstanceOf(FormData)
    expect(config?.headers?.['Content-Type']).toBe('multipart/form-data')
  })

  it('deleteAvatar posts an empty form to /users/me/avatar', async () => {
    postMock.mockResolvedValue({ data: {} })

    await deleteAvatar()

    expect(postMock).toHaveBeenCalledTimes(1)
    const [url, formData] = postMock.mock.calls[0]
    expect(url).toBe('/users/me/avatar')
    expect(formData).toBeInstanceOf(FormData)
  })

  it('security-related endpoints use /users/me/* paths', async () => {
    putMock.mockResolvedValue({ data: {} })
    deleteMock.mockResolvedValue({ data: {} })

    await updateUsername({ username: 'newname' })
    await updateEmail({ email: 'a@b.com' })
    await updatePhone({ phone: '13800000000' })
    await updatePassword({ oldPassword: 'old', newPassword: 'new' })
    await deleteAccount()

    expect(putMock).toHaveBeenCalledWith('/users/me/username', { username: 'newname' })
    expect(putMock).toHaveBeenCalledWith('/users/me/email', { email: 'a@b.com' })
    expect(putMock).toHaveBeenCalledWith('/users/me/phone', { phone: '13800000000' })
    expect(putMock).toHaveBeenCalledWith('/users/me/password', {
      oldPassword: 'old',
      newPassword: 'new',
    })
    expect(deleteMock).toHaveBeenCalledWith('/users/me')
  })
})
