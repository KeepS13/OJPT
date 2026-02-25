import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  getUserList,
  updateUser,
  deleteUser,
  updateUserStatus,
  updateUserRoles,
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  getPermissionList,
  createPermission,
  updatePermission,
  deletePermission,
  assignRolePermissions,
  getSchoolList,
  createSchool,
  updateSchool,
  deleteSchool,
  updateSchoolStatus,
  certifySchool,
  uncertifySchool,
  getPlatformStatisticsOverview,
  getUserStatistics,
  getSchoolStatistics,
} from '@/api/admin'
import type {
  UserDetail,
  UserListParams,
  UserRoleUpdateDTO,
  RoleVO,
  RoleCreateDTO,
  RoleUpdateDTO,
  PermissionVO,
  PermissionListParams,
  PermissionCreateDTO,
  PermissionUpdateDTO,
  RolePermissionAssignDTO,
  SchoolVO,
  SchoolListParams,
  SchoolCreateDTO,
  SchoolUpdateDTO,
  PlatformStatisticsOverview,
  UserStatistics,
  SchoolStatistics,
} from '@/types/admin'
import type { UserUpdateDTO } from '@/api/user'
import { useLoadingStore } from './loading'

/**
 * Admin 模块状态管理
 */
export const useAdminStore = defineStore('admin', {
  state: () => ({
    // 用户管理
    users: [] as UserDetail[],
    usersPagination: { page: 1, size: 10, total: 0 },
    usersParams: {} as UserListParams,

    // 角色管理
    roles: [] as RoleVO[],

    // 权限管理
    permissions: [] as PermissionVO[],
    permissionsParams: {} as PermissionListParams,

    // 学校管理
    schools: [] as SchoolVO[],
    schoolsPagination: { page: 1, size: 10, total: 0 },
    schoolsParams: {} as SchoolListParams,

    // 统计数据
    platformStats: null as PlatformStatisticsOverview | null,
    userStats: null as UserStatistics | null,
    schoolStats: null as SchoolStatistics | null,
  }),

  actions: {
    // ========== 用户管理 ==========

    /**
     * 获取用户列表
     */
    async fetchUsers(params?: UserListParams) {
      const loadingStore = useLoadingStore()
      loadingStore.startModuleLoading('users')

      try {
        if (params) {
          this.usersParams = { ...this.usersParams, ...params }
        }

        const res = await getUserList(this.usersParams)

        // 处理分页响应
        if (res.data && 'records' in res.data) {
          this.users = res.data.records
          this.usersPagination = {
            page: res.data.current,
            size: res.data.size,
            total: res.data.total,
          }
        } else {
          // 兼容旧格式
          this.users = res.data as unknown as UserDetail[]
          this.usersPagination.total = this.users.length
        }
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '加载用户列表失败')
        throw error
      } finally {
        loadingStore.stopModuleLoading('users')
      }
    },

    /**
     * 更新用户信息
     */
    async updateUser(userId: string, dto: UserUpdateDTO) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`update-user-${userId}`)

      try {
        await updateUser(userId, dto)
        ElMessage.success('更新成功')
        await this.fetchUsers()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
        throw error
      } finally {
        loadingStore.stopOperation(`update-user-${userId}`)
      }
    },

    /**
     * 删除用户
     */
    async deleteUser(userId: string) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`delete-user-${userId}`)

      try {
        await deleteUser(userId)
        ElMessage.success('删除成功')
        await this.fetchUsers()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
        throw error
      } finally {
        loadingStore.stopOperation(`delete-user-${userId}`)
      }
    },

    /**
     * 更新用户状态
     */
    async updateUserStatus(userId: string, status: number) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`update-user-status-${userId}`)

      try {
        await updateUserStatus(userId, { status })
        ElMessage.success('更新成功')
        await this.fetchUsers()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
        throw error
      } finally {
        loadingStore.stopOperation(`update-user-status-${userId}`)
      }
    },

    /**
     * 更新用户角色
     */
    async updateUserRoles(userId: string, dto: UserRoleUpdateDTO) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`update-user-roles-${userId}`)

      try {
        await updateUserRoles(userId, dto)
        ElMessage.success('更新成功')
        await this.fetchUsers()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
        throw error
      } finally {
        loadingStore.stopOperation(`update-user-roles-${userId}`)
      }
    },

    // ========== 角色管理 ==========

    /**
     * 获取角色列表
     */
    async fetchRoles() {
      const loadingStore = useLoadingStore()
      loadingStore.startModuleLoading('roles')

      try {
        const res = await getRoleList()
        this.roles = res.data
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '加载角色列表失败')
        throw error
      } finally {
        loadingStore.stopModuleLoading('roles')
      }
    },

    /**
     * 创建角色
     */
    async createRole(dto: RoleCreateDTO) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation('create-role')

      try {
        await createRole(dto)
        ElMessage.success('创建成功')
        await this.fetchRoles()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '创建失败')
        throw error
      } finally {
        loadingStore.stopOperation('create-role')
      }
    },

    /**
     * 更新角色
     */
    async updateRole(roleId: string, dto: RoleUpdateDTO) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`update-role-${roleId}`)

      try {
        await updateRole(roleId, dto)
        ElMessage.success('更新成功')
        await this.fetchRoles()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
        throw error
      } finally {
        loadingStore.stopOperation(`update-role-${roleId}`)
      }
    },

    /**
     * 删除角色
     */
    async deleteRole(roleId: string) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`delete-role-${roleId}`)

      try {
        await deleteRole(roleId)
        ElMessage.success('删除成功')
        await this.fetchRoles()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
        throw error
      } finally {
        loadingStore.stopOperation(`delete-role-${roleId}`)
      }
    },

    // ========== 权限管理 ==========

    /**
     * 获取权限列表
     */
    async fetchPermissions(params?: PermissionListParams) {
      const loadingStore = useLoadingStore()
      loadingStore.startModuleLoading('permissions')

      try {
        if (params) {
          this.permissionsParams = { ...this.permissionsParams, ...params }
        }

        const res = await getPermissionList(this.permissionsParams)
        this.permissions = res.data
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '加载权限列表失败')
        throw error
      } finally {
        loadingStore.stopModuleLoading('permissions')
      }
    },

    /**
     * 创建权限
     */
    async createPermission(dto: PermissionCreateDTO) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation('create-permission')

      try {
        await createPermission(dto)
        ElMessage.success('创建成功')
        await this.fetchPermissions()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '创建失败')
        throw error
      } finally {
        loadingStore.stopOperation('create-permission')
      }
    },

    /**
     * 更新权限
     */
    async updatePermission(permissionId: string, dto: PermissionUpdateDTO) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`update-permission-${permissionId}`)

      try {
        await updatePermission(permissionId, dto)
        ElMessage.success('更新成功')
        await this.fetchPermissions()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
        throw error
      } finally {
        loadingStore.stopOperation(`update-permission-${permissionId}`)
      }
    },

    /**
     * 删除权限
     */
    async deletePermission(permissionId: string) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`delete-permission-${permissionId}`)

      try {
        await deletePermission(permissionId)
        ElMessage.success('删除成功')
        await this.fetchPermissions()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
        throw error
      } finally {
        loadingStore.stopOperation(`delete-permission-${permissionId}`)
      }
    },

    /**
     * 为角色分配权限
     */
    async assignRolePermissions(roleId: string, dto: RolePermissionAssignDTO) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`assign-permissions-${roleId}`)

      try {
        await assignRolePermissions(roleId, dto)
        ElMessage.success('分配成功')
        await this.fetchRoles()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '分配失败')
        throw error
      } finally {
        loadingStore.stopOperation(`assign-permissions-${roleId}`)
      }
    },

    // ========== 学校管理 ==========

    /**
     * 获取学校列表
     */
    async fetchSchools(params?: SchoolListParams) {
      const loadingStore = useLoadingStore()
      loadingStore.startModuleLoading('schools')

      try {
        if (params) {
          this.schoolsParams = { ...this.schoolsParams, ...params }
        }

        const res = await getSchoolList(this.schoolsParams)

        // 处理分页响应
        if (res.data && 'records' in res.data) {
          this.schools = res.data.records
          this.schoolsPagination = {
            page: res.data.current,
            size: res.data.size,
            total: res.data.total,
          }
        } else {
          // 兼容旧格式
          this.schools = res.data as unknown as SchoolVO[]
          this.schoolsPagination.total = this.schools.length
        }
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '加载学校列表失败')
        throw error
      } finally {
        loadingStore.stopModuleLoading('schools')
      }
    },

    /**
     * 创建学校
     */
    async createSchool(dto: SchoolCreateDTO) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation('create-school')

      try {
        await createSchool(dto)
        ElMessage.success('创建成功')
        await this.fetchSchools()
        await this.fetchStatistics()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '创建失败')
        throw error
      } finally {
        loadingStore.stopOperation('create-school')
      }
    },

    /**
     * 更新学校
     */
    async updateSchool(schoolId: string, dto: SchoolUpdateDTO) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`update-school-${schoolId}`)

      try {
        await updateSchool(schoolId, dto)
        ElMessage.success('更新成功')
        await this.fetchSchools()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
        throw error
      } finally {
        loadingStore.stopOperation(`update-school-${schoolId}`)
      }
    },

    /**
     * 删除学校
     */
    async deleteSchool(schoolId: string) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`delete-school-${schoolId}`)

      try {
        await deleteSchool(schoolId)
        ElMessage.success('删除成功')
        await this.fetchSchools()
        await this.fetchStatistics()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
        throw error
      } finally {
        loadingStore.stopOperation(`delete-school-${schoolId}`)
      }
    },

    /**
     * 更新学校状态
     */
    async updateSchoolStatus(schoolId: string, status: number) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`update-school-status-${schoolId}`)

      try {
        await updateSchoolStatus(schoolId, { status })
        ElMessage.success('更新成功')
        await this.fetchSchools()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
        throw error
      } finally {
        loadingStore.stopOperation(`update-school-status-${schoolId}`)
      }
    },

    /**
     * 认证学校
     */
    async certifySchool(schoolId: string) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`certify-school-${schoolId}`)

      try {
        await certifySchool(schoolId)
        ElMessage.success('认证成功')
        await this.fetchSchools()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '认证失败')
        throw error
      } finally {
        loadingStore.stopOperation(`certify-school-${schoolId}`)
      }
    },

    /**
     * 取消学校认证
     */
    async uncertifySchool(schoolId: string) {
      const loadingStore = useLoadingStore()
      loadingStore.startOperation(`uncertify-school-${schoolId}`)

      try {
        await uncertifySchool(schoolId)
        ElMessage.success('取消认证成功')
        await this.fetchSchools()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '取消认证失败')
        throw error
      } finally {
        loadingStore.stopOperation(`uncertify-school-${schoolId}`)
      }
    },

    // ========== 统计数据 ==========

    /**
     * 获取统计数据
     */
    async fetchStatistics() {
      const loadingStore = useLoadingStore()
      loadingStore.startModuleLoading('statistics')

      try {
        const [platformRes, userRes, schoolRes] = await Promise.all([
          getPlatformStatisticsOverview(),
          getUserStatistics(),
          getSchoolStatistics(),
        ])
        this.platformStats = platformRes.data
        this.userStats = userRes.data
        this.schoolStats = schoolRes.data
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '加载统计数据失败')
        throw error
      } finally {
        loadingStore.stopModuleLoading('statistics')
      }
    },
  },
})
