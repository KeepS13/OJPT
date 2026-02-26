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
  getDepartments,
  createDepartment,
  getDepartmentDetail,
  updateDepartment,
  deleteDepartment,
  getSchoolClasses,
  getDepartmentClasses,
  getClassDetail,
  updateClass,
  deleteClass,
} from '../../../src/api/school'

describe('school api', () => {
  beforeEach(() => {
    getMock.mockReset()
    postMock.mockReset()
    putMock.mockReset()
    deleteMock.mockReset()
  })

  it('getDepartments 应携带分页参数调用 /school/departments', async () => {
    const params = { page: 1, size: 20 }
    getMock.mockResolvedValue({ data: {} })

    await getDepartments(params)

    expect(getMock).toHaveBeenCalledWith('/school/departments', { params })
  })

  it('createDepartment 应以 POST 方式调用 /school/departments', async () => {
    const payload = { name: 'CS', schoolId: '1' } as any
    postMock.mockResolvedValue({ data: {} })

    await createDepartment(payload)

    expect(postMock).toHaveBeenCalledWith('/school/departments', payload)
  })

  it('updateDepartment 与 deleteDepartment 应使用正确的路径', async () => {
    const id = '10'
    putMock.mockResolvedValue({ data: {} })
    deleteMock.mockResolvedValue({ data: {} })

    await getDepartmentDetail(id)
    await updateDepartment(id, { name: 'NewName' } as any)
    await deleteDepartment(id)

    expect(getMock).toHaveBeenCalledWith(`/school/departments/${id}`)
    expect(putMock).toHaveBeenCalledWith(`/school/departments/${id}`, { name: 'NewName' })
    expect(deleteMock).toHaveBeenCalledWith(`/school/departments/${id}`)
  })

  it('班级相关 API 应使用 /school/classes 前缀及关联路径', async () => {
    const classId = '100'
    const departmentId = '20'
    const params = { page: 1, size: 10 }

    getMock.mockResolvedValue({ data: {} })
    putMock.mockResolvedValue({ data: {} })
    deleteMock.mockResolvedValue({ data: {} })

    await getSchoolClasses(params)
    await getDepartmentClasses(departmentId, params)
    await getClassDetail(classId)
    await updateClass(classId, { name: 'C1' })
    await deleteClass(classId)

    expect(getMock).toHaveBeenCalledWith('/school/classes', { params })
    expect(getMock).toHaveBeenCalledWith(`/school/departments/${departmentId}/classes`, { params })
    expect(getMock).toHaveBeenCalledWith(`/school/classes/${classId}`)
    expect(putMock).toHaveBeenCalledWith(`/school/classes/${classId}`, { name: 'C1' })
    expect(deleteMock).toHaveBeenCalledWith(`/school/classes/${classId}`)
  })
})
