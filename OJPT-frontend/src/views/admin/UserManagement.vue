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
const showViewUserDialog = ref(false)
const selectedUser = ref<UserDetail | null>(null)
const userForm = ref<UserUpdateDTO>({})

const statusOptions = [
  { value: 1, label: '启用', type: 'success' },
  { value: 0, label: '禁用', type: 'danger' },
  { value: 2, label: '待审核', type: 'warning' },
]

const genderOptions = [
  { value: 0, label: '未知' },
  { value: 1, label: '男' },
  { value: 2, label: '女' },
]

const roleTypeOptions = [
  { value: 'USER', label: '用户' },
  { value: 'ADMIN', label: '管理员' },
]

const getStatusConfig = (status: number) =>
  statusOptions.find((item) => item.value === status) || { label: '未知', type: 'info' }

const getGenderLabel = (gender?: number) =>
  genderOptions.find((item) => item.value === gender)?.label || '未知'

const buildUserForm = (user: UserDetail): UserUpdateDTO => ({
  email: user.email || undefined,
  phone: user.phone || undefined,
  gender: user.gender,
  birthday: user.birthday || undefined,
  address: user.address || undefined,
  website: user.website || undefined,
  github: user.github || undefined,
  company: user.company || undefined,
  position: user.position || undefined,
  skills: user.skills || undefined,
  studentNo: user.studentNo || undefined,
  schoolId: user.schoolId || undefined,
  bio: user.bio || undefined,
  tags: user.tags || undefined,
})

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

const handleEditUser = async (userId: string | number) => {
  try {
    loading.value = true
    const res = await getUserDetail(userId)
    selectedUser.value = res.data
    userForm.value = buildUserForm(res.data)
    showEditUserDialog.value = true
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载用户详情失败')
  } finally {
    loading.value = false
  }
}

const handleViewUser = async (userId: string | number) => {
  try {
    loading.value = true
    const res = await getUserDetail(userId)
    selectedUser.value = res.data
    showViewUserDialog.value = true
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

const handleDeleteUser = async (userId: string | number) => {
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

const handleUpdateUserStatus = async (userId: string | number, status: number) => {
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

        <el-table-column label="操作" width="430" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button
                :data-testid="`view-user-${row.userId}`"
                size="small"
                @click="handleViewUser(row.userId)"
              >
                详情
              </el-button>
              <el-button
                :data-testid="`edit-user-${row.userId}`"
                size="small"
                @click="handleEditUser(row.userId)"
              >
                编辑
              </el-button>
              <el-button
                v-if="row.status !== 1"
                :data-testid="`set-status-enabled-${row.userId}`"
                size="small"
                type="success"
                @click="handleUpdateUserStatus(row.userId, 1)"
              >
                启用
              </el-button>
              <el-button
                v-if="row.status !== 0"
                :data-testid="`set-status-disabled-${row.userId}`"
                size="small"
                type="warning"
                @click="handleUpdateUserStatus(row.userId, 0)"
              >
                禁用
              </el-button>
              <el-button
                v-if="row.status !== 2"
                :data-testid="`set-status-pending-${row.userId}`"
                size="small"
                type="info"
                @click="handleUpdateUserStatus(row.userId, 2)"
              >
                待审核
              </el-button>
              <el-popconfirm
                title="确定删除该用户吗？"
                @confirm="handleDeleteUser(row.userId)"
              >
                <template #reference>
                  <el-button :data-testid="`delete-user-${row.userId}`" size="small" type="danger">
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
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

    <el-dialog v-model="showEditUserDialog" title="编辑用户" width="640px">
      <el-form :model="userForm" label-width="90px" class="user-form-grid">
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="userForm.gender" placeholder="请选择性别" clearable>
            <el-option
              v-for="item in genderOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker
            v-model="userForm.birthday"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择生日"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="userForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="个人网站">
          <el-input v-model="userForm.website" placeholder="请输入个人网站" />
        </el-form-item>
        <el-form-item label="GitHub">
          <el-input v-model="userForm.github" placeholder="请输入 GitHub" />
        </el-form-item>
        <el-form-item label="公司">
          <el-input v-model="userForm.company" placeholder="请输入公司" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="userForm.position" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="技能">
          <el-input v-model="userForm.skills" placeholder="请输入技能" />
        </el-form-item>
        <el-form-item label="学号/工号">
          <el-input v-model="userForm.studentNo" placeholder="请输入学号或工号" />
        </el-form-item>
        <el-form-item label="学校 ID">
          <el-input v-model="userForm.schoolId" placeholder="请输入学校 ID" />
        </el-form-item>
        <el-form-item label="标签" class="full-span">
          <el-input v-model="userForm.tags" placeholder="请输入标签" />
        </el-form-item>
        <el-form-item label="简介" class="full-span" data-testid="user-form-bio">
          <el-input
            v-model="userForm.bio"
            type="textarea"
            :rows="3"
            placeholder="请输入简介"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditUserDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpdateUser">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showViewUserDialog" title="用户详情" width="720px">
      <el-descriptions v-if="selectedUser" :column="2" border>
        <el-descriptions-item label="用户 ID">
          {{ selectedUser.userId }}
        </el-descriptions-item>
        <el-descriptions-item label="用户名">
          {{ selectedUser.username }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          {{ selectedUser.email || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ selectedUser.phone || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ getStatusConfig(selectedUser.status).label }}
        </el-descriptions-item>
        <el-descriptions-item label="主角色">
          {{ selectedUser.roleType || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="角色列表">
          {{ selectedUser.roles?.join(', ') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="性别">
          {{ getGenderLabel(selectedUser.gender) }}
        </el-descriptions-item>
        <el-descriptions-item label="生日">
          {{ selectedUser.birthday || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="学校 ID">
          {{ selectedUser.schoolId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="学号/工号">
          {{ selectedUser.studentNo || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="公司">
          {{ selectedUser.company || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="职位">
          {{ selectedUser.position || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="GitHub">
          {{ selectedUser.github || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="个人网站">
          {{ selectedUser.website || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="技能">
          {{ selectedUser.skills || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          {{ selectedUser.tags || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">
          {{ selectedUser.address || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">
          {{ selectedUser.bio || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ selectedUser.createdAt || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ selectedUser.updatedAt || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="showViewUserDialog = false">关闭</el-button>
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

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.user-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.user-form-grid :deep(.el-form-item) {
  margin-bottom: 18px;
}

.full-span {
  grid-column: 1 / -1;
}

@media (max-width: 768px) {
  .user-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
