<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
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
  ElPagination,
  ElCheckboxGroup,
  ElCheckbox,
  ElPopconfirm,
} from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import {
  getUserList,
  getUserDetail,
  updateUser,
  deleteUser,
  updateUserStatus,
  updateUserRoles,
  getRoleList,
} from '@/api/admin'
import type {
  UserDetail,
  UserListParams,
  UserRoleUpdateDTO,
  RoleVO,
} from '@/types/admin'
import type { UserUpdateDTO } from '@/api/user'

const loading = ref(false)

// 用户列表数据
const users = ref<UserDetail[]>([])
const userParams = ref<UserListParams>({ page: 1, size: 10 })
const totalUsers = ref(0)

// 编辑用户相关
const showEditUserDialog = ref(false)
const selectedUser = ref<UserDetail | null>(null)
const userForm = ref<UserUpdateDTO>({})

// 用户角色管理相关
const showUserRolesDialog = ref(false)
const userRolesForm = ref<UserRoleUpdateDTO>({ roleCodes: [] })
const availableRoles = ref<RoleVO[]>([])

// 状态映射
const statusOptions = [
  { value: 1, label: '启用', type: 'success' },
  { value: 0, label: '禁用', type: 'danger' },
  { value: 2, label: '待审核', type: 'warning' },
]

// 角色类型选项
const roleTypeOptions = [
  { value: 'USER', label: '学员' },
  { value: 'STUDENT', label: '学生' },
  { value: 'TEACHER', label: '教师' },
  { value: 'SCHOOL', label: '校方' },
  { value: 'ADMIN', label: '管理员' },
]

// 获取状态标签配置
const getStatusConfig = (status: number) => {
  return statusOptions.find(s => s.value === status) || { label: '未知', type: 'info' }
}

// 加载用户列表
const loadUsers = async () => {
  try {
    loading.value = true
    const res = await getUserList(userParams.value)
    // 处理新的分页响应格式
    if (res.data && 'records' in res.data) {
      users.value = res.data.records
      totalUsers.value = res.data.total
    } else {
      // 兼容旧格式
      users.value = res.data as unknown as UserDetail[]
      totalUsers.value = users.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 重置筛选条件
const resetFilters = () => {
  userParams.value = { page: 1, size: 10 }
  loadUsers()
}

// 分页变化处理
const handlePageChange = (page: number) => {
  userParams.value.page = page
  loadUsers()
}

const handleSizeChange = (size: number) => {
  userParams.value.size = size
  userParams.value.page = 1
  loadUsers()
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
    
    // 加载可用角色列表
    const rolesRes = await getRoleList()
    availableRoles.value = rolesRes.data
    
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

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="user-management">
    <!-- 筛选栏 -->
    <div class="filter-section">
      <div class="filter-row">
        <el-input
          v-model="userParams.keyword"
          placeholder="搜索用户名/邮箱/手机号"
          style="width: 240px"
          clearable
          @keyup.enter="loadUsers"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select 
          v-model="userParams.status" 
          placeholder="状态" 
          style="width: 120px" 
          clearable
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select 
          v-model="userParams.roleType" 
          placeholder="角色类型" 
          style="width: 120px" 
          clearable
        >
          <el-option
            v-for="item in roleTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="loadUsers">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="resetFilters">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-table :data="users" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusConfig(row.status).type as any">
              {{ getStatusConfig(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="200">
          <template #default="{ row }">
            <el-tag 
              v-for="role in row.roles" 
              :key="role" 
              style="margin-right: 4px; margin-bottom: 4px"
              size="small"
            >
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEditUser(row.userId)">编辑</el-button>
            <el-button size="small" type="primary" @click="handleEditUserRoles(row.userId)">
              角色
            </el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleUpdateUserStatus(row.userId, row.status === 1 ? 0 : 1)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm
              title="确定要删除该用户吗？"
              @confirm="handleDeleteUser(row.userId)"
            >
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="userParams.page"
          v-model:page-size="userParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalUsers"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="showEditUserDialog" title="编辑用户" width="500px">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="userForm.studentNo" placeholder="请输入学号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditUserDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpdateUser">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用户角色管理弹窗 -->
    <el-dialog v-model="showUserRolesDialog" title="用户角色管理" width="500px">
      <el-form :model="userRolesForm" label-width="80px">
        <el-form-item label="角色">
          <el-checkbox-group v-model="userRolesForm.roleCodes">
            <el-checkbox 
              v-for="role in availableRoles" 
              :key="role.code" 
              :label="role.code"
              :value="role.code"
            >
              {{ role.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUserRolesDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpdateUserRoles">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-section {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.table-section {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
