<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  ElMessage,
  ElDialog,
  ElTable,
  ElTableColumn,
  ElButton,
  ElTag,
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElOption,
  ElTabs,
  ElTabPane,
  ElCard,
  ElRow,
  ElCol,
  ElPagination,
  ElCheckboxGroup,
  ElCheckbox,
} from 'element-plus'
import {
  getUserList,
  getUserDetail,
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
  removeRolePermission,
  getRoleDetail,
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
  UserStatusUpdateDTO,
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
  SchoolStatusUpdateDTO,
  PlatformStatisticsOverview,
  UserStatistics,
  SchoolStatistics,
} from '@/types/admin'
import type { UserUpdateDTO } from '@/api/user'

const loading = ref(false)
const activeTab = ref('overview')

// 统计数据
const platformStats = ref<PlatformStatisticsOverview | null>(null)
const userStats = ref<UserStatistics | null>(null)
const schoolStats = ref<SchoolStatistics | null>(null)

// 用户管理
const users = ref<UserDetail[]>([])
const userParams = ref<UserListParams>({ page: 1, size: 10 })
const totalUsers = ref(0)
const showEditUserDialog = ref(false)
const showUserRolesDialog = ref(false)
const selectedUser = ref<UserDetail | null>(null)
const userForm = ref<UserUpdateDTO>({})
const userRolesForm = ref<UserRoleUpdateDTO>({ roleCodes: [] })
const availableRoles = ref<RoleVO[]>([])

// 角色管理
const roles = ref<RoleVO[]>([])
const showCreateRoleDialog = ref(false)
const showEditRoleDialog = ref(false)
const showRolePermissionsDialog = ref(false)
const selectedRole = ref<RoleVO | null>(null)
const roleForm = ref<RoleCreateDTO>({ code: '', name: '', description: '', level: 0 })
const rolePermissions = ref<string[]>([])
const allPermissions = ref<PermissionVO[]>([])

// 权限管理
const permissions = ref<PermissionVO[]>([])
const permissionParams = ref<PermissionListParams>({})
const showCreatePermissionDialog = ref(false)
const showEditPermissionDialog = ref(false)
const selectedPermission = ref<PermissionVO | null>(null)
const permissionForm = ref<PermissionCreateDTO>({ resource: '', action: '', description: '' })

// 学校管理
const schools = ref<SchoolVO[]>([])
const schoolParams = ref<SchoolListParams>({ page: 1, size: 10 })
const totalSchools = ref(0)
const showCreateSchoolDialog = ref(false)
const showEditSchoolDialog = ref(false)
const selectedSchool = ref<SchoolVO | null>(null)
const schoolForm = ref<SchoolCreateDTO>({ name: '', contact: '', status: 1 })

// 加载统计数据
const loadStatistics = async () => {
  try {
    loading.value = true
    const [platformRes, userRes, schoolRes] = await Promise.all([
      getPlatformStatisticsOverview(),
      getUserStatistics(),
      getSchoolStatistics(),
    ])
    platformStats.value = platformRes.data
    userStats.value = userRes.data
    schoolStats.value = schoolRes.data
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// 加载用户列表
const loadUsers = async () => {
  try {
    loading.value = true
    const res = await getUserList(userParams.value)
    const data = res.data as { records?: UserDetail[]; total?: number } | UserDetail[]
    if (data && typeof data === 'object' && 'records' in data && Array.isArray(data.records)) {
      users.value = data.records
      totalUsers.value = data.total ?? 0
    } else {
      users.value = Array.isArray(data) ? data : []
      totalUsers.value = users.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 打开编辑用户弹窗
const handleEditUser = async (userId: string) => {
  try {
    loading.value = true
    const res = await getUserDetail(userId)
    selectedUser.value = res.data
    userForm.value = {
      email: res.data.email,
      phone: res.data.phone,
      studentNo: res.data.studentNo,
    }
    showEditUserDialog.value = true
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载用户详情失败')
  } finally {
    loading.value = false
  }
}

// 提交更新用户
const submitUpdateUser = async () => {
  if (!selectedUser.value) return
  try {
    loading.value = true
    await updateUser(selectedUser.value.userId, userForm.value)
    ElMessage.success('更新成功')
    showEditUserDialog.value = false
    await loadUsers()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 删除用户
const handleDeleteUser = async (userId: string) => {
  try {
    loading.value = true
    await deleteUser(userId)
    ElMessage.success('删除成功')
    await loadUsers()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
  } finally {
    loading.value = false
  }
}

// 更新用户状态
const handleUpdateUserStatus = async (userId: string, status: number) => {
  try {
    loading.value = true
    await updateUserStatus(userId, { status })
    ElMessage.success('更新成功')
    await loadUsers()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 打开用户角色管理弹窗
const handleEditUserRoles = async (userId: string) => {
  try {
    loading.value = true
    const res = await getUserDetail(userId)
    selectedUser.value = res.data
    userRolesForm.value = { roleCodes: res.data.roles || [] }
    await loadRoles()
    showUserRolesDialog.value = true
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载用户详情失败')
  } finally {
    loading.value = false
  }
}

// 提交更新用户角色
const submitUpdateUserRoles = async () => {
  if (!selectedUser.value) return
  try {
    loading.value = true
    await updateUserRoles(selectedUser.value.userId, userRolesForm.value)
    ElMessage.success('更新成功')
    showUserRolesDialog.value = false
    await loadUsers()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 加载角色列表
const loadRoles = async () => {
  try {
    loading.value = true
    const res = await getRoleList()
    roles.value = res.data
    availableRoles.value = res.data
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载角色列表失败')
  } finally {
    loading.value = false
  }
}

// 打开创建角色弹窗
const handleCreateRole = () => {
  roleForm.value = { code: '', name: '', description: '', level: 0 }
  showCreateRoleDialog.value = true
}

// 提交创建角色
const submitCreateRole = async () => {
  try {
    loading.value = true
    await createRole(roleForm.value)
    ElMessage.success('创建成功')
    showCreateRoleDialog.value = false
    await loadRoles()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '创建失败')
  } finally {
    loading.value = false
  }
}

// 打开编辑角色弹窗
const handleEditRole = (role: RoleVO) => {
  selectedRole.value = role
  roleForm.value = {
    code: role.code,
    name: role.name,
    description: role.description || '',
    level: role.level,
  }
  showEditRoleDialog.value = true
}

// 提交更新角色
const submitUpdateRole = async () => {
  if (!selectedRole.value) return
  try {
    loading.value = true
    const payload: RoleUpdateDTO = {
      name: roleForm.value.name,
      description: roleForm.value.description,
      level: roleForm.value.level,
    }
    await updateRole(selectedRole.value.id, payload)
    ElMessage.success('更新成功')
    showEditRoleDialog.value = false
    await loadRoles()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 删除角色
const handleDeleteRole = async (roleId: string) => {
  try {
    loading.value = true
    await deleteRole(roleId)
    ElMessage.success('删除成功')
    await loadRoles()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
  } finally {
    loading.value = false
  }
}

// 打开角色权限管理弹窗
const handleEditRolePermissions = async (roleId: string) => {
  try {
    loading.value = true
    const res = await getRoleDetail(roleId)
    selectedRole.value = res.data
    // 确保权限ID是字符串类型
    rolePermissions.value = res.data.permissions?.map((p) => 
      typeof p.id === 'number' ? String(p.id) : p.id
    ) || []
    await loadPermissions()
    showRolePermissionsDialog.value = true
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载角色详情失败')
  } finally {
    loading.value = false
  }
}

// 提交更新角色权限
const submitUpdateRolePermissions = async () => {
  if (!selectedRole.value) return
  try {
    loading.value = true
    // 确保permissionIds是字符串数组
    const permissionIds = rolePermissions.value.map(id => typeof id === 'number' ? String(id) : id)
    await assignRolePermissions(selectedRole.value.id, { permissionIds })
    ElMessage.success('更新成功')
    showRolePermissionsDialog.value = false
    await loadRoles()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 加载权限列表
const loadPermissions = async () => {
  try {
    loading.value = true
    const res = await getPermissionList(permissionParams.value)
    permissions.value = res.data
    allPermissions.value = res.data
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载权限列表失败')
  } finally {
    loading.value = false
  }
}

// 打开创建权限弹窗
const handleCreatePermission = () => {
  permissionForm.value = { resource: '', action: '', description: '' }
  showCreatePermissionDialog.value = true
}

// 提交创建权限
const submitCreatePermission = async () => {
  try {
    loading.value = true
    await createPermission(permissionForm.value)
    ElMessage.success('创建成功')
    showCreatePermissionDialog.value = false
    await loadPermissions()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '创建失败')
  } finally {
    loading.value = false
  }
}

// 打开编辑权限弹窗
const handleEditPermission = (permission: PermissionVO) => {
  selectedPermission.value = permission
  permissionForm.value = {
    resource: permission.resource,
    action: permission.action,
    description: permission.description || '',
  }
  showEditPermissionDialog.value = true
}

// 提交更新权限
const submitUpdatePermission = async () => {
  if (!selectedPermission.value) return
  try {
    loading.value = true
    const payload: PermissionUpdateDTO = {
      description: permissionForm.value.description,
    }
    await updatePermission(selectedPermission.value.id, payload)
    ElMessage.success('更新成功')
    showEditPermissionDialog.value = false
    await loadPermissions()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 删除权限
const handleDeletePermission = async (permissionId: string) => {
  try {
    loading.value = true
    await deletePermission(permissionId)
    ElMessage.success('删除成功')
    await loadPermissions()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
  } finally {
    loading.value = false
  }
}

// 加载学校列表
const loadSchools = async () => {
  try {
    loading.value = true
    const res = await getSchoolList(schoolParams.value)
    const data = res.data as { records?: SchoolVO[]; total?: number } | SchoolVO[]
    if (data && typeof data === 'object' && 'records' in data && Array.isArray(data.records)) {
      schools.value = data.records
      totalSchools.value = data.total ?? 0
    } else {
      schools.value = Array.isArray(data) ? data : []
      totalSchools.value = schools.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载学校列表失败')
  } finally {
    loading.value = false
  }
}

// 打开创建学校弹窗
const handleCreateSchool = () => {
  schoolForm.value = { name: '', contact: '', status: 1 }
  showCreateSchoolDialog.value = true
}

// 提交创建学校
const submitCreateSchool = async () => {
  try {
    loading.value = true
    await createSchool(schoolForm.value)
    ElMessage.success('创建成功')
    showCreateSchoolDialog.value = false
    await loadSchools()
    await loadStatistics()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '创建失败')
  } finally {
    loading.value = false
  }
}

// 打开编辑学校弹窗
const handleEditSchool = (school: SchoolVO) => {
  selectedSchool.value = school
  schoolForm.value = {
    name: school.name,
    contact: school.contact || '',
    status: school.status,
  }
  showEditSchoolDialog.value = true
}

// 提交更新学校
const submitUpdateSchool = async () => {
  if (!selectedSchool.value) return
  try {
    loading.value = true
    await updateSchool(selectedSchool.value.id, schoolForm.value)
    ElMessage.success('更新成功')
    showEditSchoolDialog.value = false
    await loadSchools()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 删除学校
const handleDeleteSchool = async (schoolId: string) => {
  try {
    loading.value = true
    await deleteSchool(schoolId)
    ElMessage.success('删除成功')
    await loadSchools()
    await loadStatistics()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
  } finally {
    loading.value = false
  }
}

// 更新学校状态
const handleUpdateSchoolStatus = async (schoolId: string, status: number) => {
  try {
    loading.value = true
    await updateSchoolStatus(schoolId, { status })
    ElMessage.success('更新成功')
    await loadSchools()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 认证学校
const handleCertifySchool = async (schoolId: string) => {
  try {
    loading.value = true
    await certifySchool(schoolId)
    ElMessage.success('认证成功')
    await loadSchools()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '认证失败')
  } finally {
    loading.value = false
  }
}

// 取消认证
const handleUncertifySchool = async (schoolId: string) => {
  try {
    loading.value = true
    await uncertifySchool(schoolId)
    ElMessage.success('取消认证成功')
    await loadSchools()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '取消认证失败')
  } finally {
    loading.value = false
  }
}

// 格式化状态
const formatStatus = (status: number): { text: string; type: 'success' | 'warning' | 'danger' | 'info' } => {
  const map: Record<number, { text: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
    0: { text: '禁用', type: 'danger' },
    1: { text: '启用', type: 'success' },
    2: { text: '待审核', type: 'warning' },
  }
  return map[status] || { text: '未知', type: 'info' }
}

onMounted(() => {
  loadStatistics()
  loadUsers()
  loadRoles()
  loadPermissions()
  loadSchools()
})
</script>

<template>
  <div class="admin-view">
    <div class="admin-header">
      <h1 class="admin-title">管理员控制台</h1>
      <p class="admin-subtitle">管理平台用户、角色权限和学校</p>
    </div>

    <div class="admin-content">
      <el-tabs v-model="activeTab">
        <!-- 数据概览 -->
        <el-tab-pane label="数据概览" name="overview">
          <div class="section">
            <h2 class="section-title">平台统计</h2>
            <el-row :gutter="20" v-if="platformStats">
              <el-col :span="12">
                <el-card class="stat-card">
                  <div class="stat-value">{{ platformStats.statusCount.users }}</div>
                  <div class="stat-label">用户总数</div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card class="stat-card">
                  <div class="stat-value">{{ platformStats.statusCount.schools }}</div>
                  <div class="stat-label">学校总数</div>
                </el-card>
              </el-col>
            </el-row>

            <h2 class="section-title" style="margin-top: 32px">用户统计</h2>
            <el-row :gutter="20" v-if="userStats">
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-value">{{ userStats.statusCount['0'] || 0 }}</div>
                  <div class="stat-label">禁用用户</div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-value">{{ userStats.statusCount['1'] || 0 }}</div>
                  <div class="stat-label">启用用户</div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-value">{{ userStats.statusCount['2'] || 0 }}</div>
                  <div class="stat-label">待审核用户</div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-value">{{ userStats.totalCount }}</div>
                  <div class="stat-label">用户总数</div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- 用户管理 -->
        <el-tab-pane label="用户管理" name="users">
          <div class="section">
            <div class="section-header">
              <h2 class="section-title">用户列表</h2>
              <div class="filter-bar">
                <el-input
                  v-model="userParams.keyword"
                  placeholder="搜索用户名/邮箱/手机号"
                  style="width: 200px; margin-right: 10px"
                  @keyup.enter="loadUsers"
                />
                <el-select v-model="userParams.status" placeholder="状态" style="width: 120px; margin-right: 10px" clearable>
                  <el-option label="启用" :value="1" />
                  <el-option label="禁用" :value="0" />
                  <el-option label="待审核" :value="2" />
                </el-select>
                <el-select v-model="userParams.roleType" placeholder="角色" style="width: 120px; margin-right: 10px" clearable>
                  <el-option label="学员" value="USER" />
                  <el-option label="教师" value="TEACHER" />
                  <el-option label="校方" value="SCHOOL" />
                  <el-option label="管理员" value="ADMIN" />
                </el-select>
                <el-button type="primary" @click="loadUsers">搜索</el-button>
              </div>
            </div>
            <el-table :data="users" v-loading="loading" style="width: 100%">
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="email" label="邮箱" width="200" />
              <el-table-column prop="phone" label="手机号" width="120" />
              <el-table-column label="角色" width="150">
                <template #default="{ row }">
                  <el-tag v-for="role in row.roles" :key="role" style="margin-right: 5px">
                    {{ role }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="300" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="handleEditUser(row.userId)">编辑</el-button>
                  <el-button size="small" @click="handleEditUserRoles(row.userId)">角色</el-button>
                  <el-button
                    size="small"
                    :type="row.status === 1 ? 'warning' : 'success'"
                    @click="handleUpdateUserStatus(row.userId, row.status === 1 ? 0 : 1)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                  <el-button size="small" type="danger" @click="handleDeleteUser(row.userId)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="userParams.page"
              v-model:page-size="userParams.size"
              :total="totalUsers"
              layout="total, prev, pager, next"
              @current-change="loadUsers"
              style="margin-top: 16px"
            />
          </div>
        </el-tab-pane>

        <!-- 角色管理 -->
        <el-tab-pane label="角色管理" name="roles">
          <div class="section">
            <div class="section-header">
              <h2 class="section-title">角色列表</h2>
              <el-button type="primary" @click="handleCreateRole">创建角色</el-button>
            </div>
            <el-table :data="roles" v-loading="loading" style="width: 100%">
              <el-table-column prop="code" label="角色编码" width="150" />
              <el-table-column prop="name" label="角色名称" width="150" />
              <el-table-column prop="description" label="描述" />
              <el-table-column prop="level" label="层级" width="100" />
              <el-table-column prop="permissionCount" label="权限数" width="100" />
              <el-table-column label="操作" width="250" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="handleEditRole(row)">编辑</el-button>
                  <el-button size="small" type="primary" @click="handleEditRolePermissions(row.id)">
                    权限
                  </el-button>
                  <el-button size="small" type="danger" @click="handleDeleteRole(row.id)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 权限管理 -->
        <el-tab-pane label="权限管理" name="permissions">
          <div class="section">
            <div class="section-header">
              <h2 class="section-title">权限列表</h2>
              <div class="filter-bar">
                <el-input
                  v-model="permissionParams.keyword"
                  placeholder="搜索权限"
                  style="width: 200px; margin-right: 10px"
                  @keyup.enter="loadPermissions"
                />
                <el-button type="primary" @click="loadPermissions">搜索</el-button>
                <el-button type="primary" @click="handleCreatePermission" style="margin-left: 10px">
                  创建权限
                </el-button>
              </div>
            </div>
            <el-table :data="permissions" v-loading="loading" style="width: 100%">
              <el-table-column prop="resource" label="资源" width="200" />
              <el-table-column prop="action" label="操作" width="100" />
              <el-table-column prop="description" label="描述" />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="handleEditPermission(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="handleDeletePermission(row.id)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 学校管理 -->
        <el-tab-pane label="学校管理" name="schools">
          <div class="section">
            <div class="section-header">
              <h2 class="section-title">学校列表</h2>
              <el-button type="primary" @click="handleCreateSchool">创建学校</el-button>
            </div>
            <el-table :data="schools" v-loading="loading" style="width: 100%">
              <el-table-column prop="name" label="学校名称" width="200" />
              <el-table-column prop="contact" label="联系方式" width="150" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="formatStatus(row.status).type">
                    {{ formatStatus(row.status).text }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="350" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="handleEditSchool(row)">编辑</el-button>
                  <el-button
                    size="small"
                    :type="row.status === 1 ? 'warning' : 'success'"
                    @click="handleUpdateSchoolStatus(row.id, row.status === 1 ? 0 : 1)"
                  >
                    {{ row.status === 1 ? '禁用' : '启用' }}
                  </el-button>
                  <el-button
                    v-if="row.status === 2"
                    size="small"
                    type="success"
                    @click="handleCertifySchool(row.id)"
                  >
                    认证
                  </el-button>
                  <el-button
                    v-if="row.status === 1"
                    size="small"
                    type="warning"
                    @click="handleUncertifySchool(row.id)"
                  >
                    取消认证
                  </el-button>
                  <el-button size="small" type="danger" @click="handleDeleteSchool(row.id)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 编辑用户弹窗 -->
    <ElDialog v-model="showEditUserDialog" title="编辑用户" width="500px">
      <el-form :model="userForm" label-width="100px">
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="userForm.studentNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditUserDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdateUser">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 用户角色管理弹窗 -->
    <ElDialog v-model="showUserRolesDialog" title="用户角色管理" width="500px">
      <el-form :model="userRolesForm" label-width="100px">
        <el-form-item label="角色">
          <el-checkbox-group v-model="userRolesForm.roleCodes">
            <el-checkbox v-for="role in availableRoles" :key="role.code" :label="role.code">
              {{ role.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showUserRolesDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdateUserRoles">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 创建角色弹窗 -->
    <ElDialog v-model="showCreateRoleDialog" title="创建角色" width="500px">
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="角色编码">
          <el-input v-model="roleForm.code" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="roleForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="层级">
          <el-input v-model.number="roleForm.level" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateRoleDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitCreateRole">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 编辑角色弹窗 -->
    <ElDialog v-model="showEditRoleDialog" title="编辑角色" width="500px">
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="角色名称">
          <el-input v-model="roleForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="层级">
          <el-input v-model.number="roleForm.level" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditRoleDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdateRole">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 角色权限管理弹窗 -->
    <ElDialog v-model="showRolePermissionsDialog" title="角色权限管理" width="600px">
      <el-checkbox-group v-model="rolePermissions" style="display: flex; flex-direction: column; gap: 10px">
        <el-checkbox v-for="permission in allPermissions" :key="permission.id" :label="permission.id">
          {{ permission.resource }} - {{ permission.action }} ({{ permission.description }})
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showRolePermissionsDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdateRolePermissions">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 创建权限弹窗 -->
    <ElDialog v-model="showCreatePermissionDialog" title="创建权限" width="500px">
      <el-form :model="permissionForm" label-width="100px">
        <el-form-item label="资源">
          <el-input v-model="permissionForm.resource" />
        </el-form-item>
        <el-form-item label="操作">
          <el-input v-model="permissionForm.action" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="permissionForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreatePermissionDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitCreatePermission">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 编辑权限弹窗 -->
    <ElDialog v-model="showEditPermissionDialog" title="编辑权限" width="500px">
      <el-form :model="permissionForm" label-width="100px">
        <el-form-item label="资源">
          <el-input v-model="permissionForm.resource" disabled />
        </el-form-item>
        <el-form-item label="操作">
          <el-input v-model="permissionForm.action" disabled />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="permissionForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditPermissionDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdatePermission">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 创建学校弹窗 -->
    <ElDialog v-model="showCreateSchoolDialog" title="创建学校" width="500px">
      <el-form :model="schoolForm" label-width="100px">
        <el-form-item label="学校名称">
          <el-input v-model="schoolForm.name" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="schoolForm.contact" />
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model.number="schoolForm.status" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateSchoolDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitCreateSchool">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 编辑学校弹窗 -->
    <ElDialog v-model="showEditSchoolDialog" title="编辑学校" width="500px">
      <el-form :model="schoolForm" label-width="100px">
        <el-form-item label="学校名称">
          <el-input v-model="schoolForm.name" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="schoolForm.contact" />
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model.number="schoolForm.status" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditSchoolDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdateSchool">确定</el-button>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.admin-view {
  max-width: 1400px;
  margin: 0 auto;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.admin-header {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.admin-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.admin-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.admin-content {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.section {
  width: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 16px 0;
}

.filter-bar {
  display: flex;
  align-items: center;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  color: #2563eb;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-view {
    padding: 20px;
    border-radius: 8px;
  }
}
</style>



