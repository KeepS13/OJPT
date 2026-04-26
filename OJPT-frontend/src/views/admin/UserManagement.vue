<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  deleteUser,
  getUserDetail,
  getUserList,
  updateUser,
  updateUserStatus,
} from '@/api/admin'
import type { UserUpdateDTO } from '@/api/user'
import type { UserDetail, UserListParams } from '@/types/admin'

const loading = ref(false)
const users = ref<UserDetail[]>([])
const totalUsers = ref(0)
const userParams = ref<UserListParams>({ page: 1, size: 10 })

const showEditUserDialog = ref(false)
const selectedUser = ref<UserDetail | null>(null)
const userForm = ref<UserUpdateDTO>({})

const statusOptions = [
  { value: 1, label: '启用', type: 'success' },
  { value: 0, label: '禁用', type: 'danger' },
  { value: 2, label: '待审核', type: 'warning' },
]

const roleTypeOptions = [
  { value: 'USER', label: '用户' },
  { value: 'ADMIN', label: '管理员' },
]

const getStatusConfig = (status: number) =>
  statusOptions.find((item) => item.value === status) || { label: '未知', type: 'info' }

const loadUsers = async () => {
  try {
    loading.value = true
    const res = await getUserList(userParams.value)
    if (res.data && 'records' in res.data) {
      users.value = res.data.records
      totalUsers.value = res.data.total
      return
    }

    users.value = res.data as unknown as UserDetail[]
    totalUsers.value = users.value.length
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  userParams.value = { page: 1, size: 10 }
  loadUsers()
}

const handlePageChange = (page: number) => {
  userParams.value.page = page
  loadUsers()
}

const handleSizeChange = (size: number) => {
  userParams.value.size = size
  userParams.value.page = 1
  loadUsers()
}

const handleEditUser = async (userId: string) => {
  try {
    loading.value = true
    const res = await getUserDetail(userId)
    selectedUser.value = res.data
    userForm.value = {
      email: res.data.email,
      phone: res.data.phone,
    }
    showEditUserDialog.value = true
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载用户详情失败')
  } finally {
    loading.value = false
  }
}

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

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="user-management">
    <div class="filter-section">
      <div class="filter-row">
        <el-input
          v-model="userParams.keyword"
          placeholder="搜索用户名、邮箱或手机号"
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
          placeholder="角色"
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

    <div class="table-section">
      <el-table v-loading="loading" :data="users" style="width: 100%">
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column prop="phone" label="手机号" width="160" />

        <el-table-column label="状态" width="110">
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
              size="small"
              class="role-tag"
            >
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEditUser(row.userId)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleUpdateUserStatus(row.userId, row.status === 1 ? 0 : 1)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm
              title="确定删除该用户吗？"
              @confirm="handleDeleteUser(row.userId)"
            >
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog v-model="showEditUserDialog" title="编辑用户" width="500px">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditUserDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpdateUser">确定</el-button>
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

.filter-section,
.table-section {
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

.role-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
