<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  ElMessage,
  ElDialog,
  ElTable,
  ElTableColumn,
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElPopconfirm,
} from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import {
  getPermissionList,
  createPermission,
  updatePermission,
  deletePermission,
} from '@/api/admin'
import type {
  PermissionVO,
  PermissionListParams,
  PermissionCreateDTO,
  PermissionUpdateDTO,
} from '@/types/admin'

const loading = ref(false)

// 权限列表数据
const permissions = ref<PermissionVO[]>([])
const permissionParams = ref<PermissionListParams>({})

// 创建/编辑权限相关
const showCreatePermissionDialog = ref(false)
const showEditPermissionDialog = ref(false)
const selectedPermission = ref<PermissionVO | null>(null)
const permissionForm = ref<PermissionCreateDTO>({ resource: '', action: '', description: '' })

// 加载权限列表
const loadPermissions = async () => {
  try {
    loading.value = true
    const res = await getPermissionList(permissionParams.value)
    permissions.value = res.data
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载权限列表失败')
  } finally {
    loading.value = false
  }
}

// 重置筛选
const resetFilters = () => {
  permissionParams.value = {}
  loadPermissions()
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

onMounted(() => {
  loadPermissions()
})
</script>

<template>
  <div class="permission-management">
    <!-- 筛选和操作栏 -->
    <div class="filter-section">
      <div class="filter-row">
        <el-input
          v-model="permissionParams.keyword"
          placeholder="搜索权限"
          style="width: 240px"
          clearable
          @keyup.enter="loadPermissions"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-input
          v-model="permissionParams.resource"
          placeholder="资源"
          style="width: 160px"
          clearable
        />
        <el-input
          v-model="permissionParams.action"
          placeholder="操作"
          style="width: 120px"
          clearable
        />
        <el-button type="primary" @click="loadPermissions">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="resetFilters">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
        <div style="flex: 1"></div>
        <el-button type="primary" @click="handleCreatePermission">
          <el-icon><Plus /></el-icon>
          创建权限
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-table :data="permissions" v-loading="loading" style="width: 100%">
        <el-table-column prop="resource" label="资源" width="250">
          <template #default="{ row }">
            <code class="resource-code">{{ row.resource }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="120">
          <template #default="{ row }">
            <span class="action-tag">{{ row.action }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEditPermission(row)">编辑</el-button>
            <el-popconfirm
              title="确定要删除该权限吗？"
              @confirm="handleDeletePermission(row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建权限弹窗 -->
    <el-dialog v-model="showCreatePermissionDialog" title="创建权限" width="500px">
      <el-form :model="permissionForm" label-width="80px">
        <el-form-item label="资源" required>
          <el-input v-model="permissionForm.resource" placeholder="如 /api/users" />
        </el-form-item>
        <el-form-item label="操作" required>
          <el-input v-model="permissionForm.action" placeholder="如 GET、POST、DELETE" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="permissionForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreatePermissionDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitCreatePermission">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑权限弹窗 -->
    <el-dialog v-model="showEditPermissionDialog" title="编辑权限" width="500px">
      <el-form :model="permissionForm" label-width="80px">
        <el-form-item label="资源">
          <el-input v-model="permissionForm.resource" disabled />
        </el-form-item>
        <el-form-item label="操作">
          <el-input v-model="permissionForm.action" disabled />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="permissionForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditPermissionDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpdatePermission">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.permission-management {
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

.resource-code {
  font-family: 'SF Mono', Monaco, 'Courier New', monospace;
  font-size: 13px;
  color: #111827;
  background-color: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
}

.action-tag {
  display: inline-block;
  font-family: 'SF Mono', Monaco, 'Courier New', monospace;
  font-size: 12px;
  font-weight: 500;
  color: #2563eb;
  background-color: #eff6ff;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>
