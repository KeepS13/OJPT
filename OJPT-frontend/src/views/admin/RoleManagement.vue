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
  ElCheckboxGroup,
  ElCheckbox,
  ElPopconfirm,
} from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  getRoleDetail,
  assignRolePermissions,
  getPermissionList,
} from '@/api/admin'
import type {
  RoleVO,
  RoleCreateDTO,
  RoleUpdateDTO,
  PermissionVO,
} from '@/types/admin'

const loading = ref(false)

// 角色列表数据
const roles = ref<RoleVO[]>([])

// 创建/编辑角色相关
const showCreateRoleDialog = ref(false)
const showEditRoleDialog = ref(false)
const selectedRole = ref<RoleVO | null>(null)
const roleForm = ref<RoleCreateDTO>({ code: '', name: '', description: '', level: 0 })

// 角色权限管理相关
const showRolePermissionsDialog = ref(false)
const rolePermissions = ref<string[]>([])
const allPermissions = ref<PermissionVO[]>([])

// 加载角色列表
const loadRoles = async () => {
  try {
    loading.value = true
    const res = await getRoleList()
    roles.value = res.data
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
    const [roleRes, permissionsRes] = await Promise.all([
      getRoleDetail(roleId),
      getPermissionList({}),
    ])
    selectedRole.value = roleRes.data
    // 确保权限ID是字符串类型
    rolePermissions.value = roleRes.data.permissions?.map((p) => 
      typeof p.id === 'number' ? String(p.id) : p.id
    ) || []
    allPermissions.value = permissionsRes.data
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

onMounted(() => {
  loadRoles()
})
</script>

<template>
  <div class="role-management">
    <!-- 操作栏 -->
    <div class="action-section">
      <el-button type="primary" @click="handleCreateRole">
        <el-icon><Plus /></el-icon>
        创建角色
      </el-button>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-table :data="roles" v-loading="loading" style="width: 100%">
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="level" label="层级" width="80" />
        <el-table-column prop="permissionCount" label="权限数" width="100" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEditRole(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="handleEditRolePermissions(row.id)">
              权限
            </el-button>
            <el-popconfirm
              title="确定要删除该角色吗？"
              @confirm="handleDeleteRole(row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 创建角色弹窗 -->
    <el-dialog v-model="showCreateRoleDialog" title="创建角色" width="500px">
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="编码" required>
          <el-input v-model="roleForm.code" placeholder="请输入角色编码（如 ADMIN）" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="层级">
          <el-input v-model.number="roleForm.level" type="number" placeholder="请输入层级（数字越大优先级越高）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateRoleDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitCreateRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑角色弹窗 -->
    <el-dialog v-model="showEditRoleDialog" title="编辑角色" width="500px">
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="编码">
          <el-input v-model="roleForm.code" disabled />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="层级">
          <el-input v-model.number="roleForm.level" type="number" placeholder="请输入层级" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditRoleDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpdateRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- 角色权限管理弹窗 -->
    <el-dialog v-model="showRolePermissionsDialog" title="角色权限管理" width="700px">
      <div class="permissions-list">
        <el-checkbox-group v-model="rolePermissions">
          <div v-for="permission in allPermissions" :key="permission.id" class="permission-item">
            <el-checkbox :label="permission.id" :value="permission.id">
              <span class="permission-resource">{{ permission.resource }}</span>
              <span class="permission-action">{{ permission.action }}</span>
              <span class="permission-desc" v-if="permission.description">
                ({{ permission.description }})
              </span>
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="showRolePermissionsDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpdateRolePermissions">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-section {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.table-section {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.permissions-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
}

.permission-item {
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.permission-item:last-child {
  border-bottom: none;
}

.permission-resource {
  font-weight: 500;
  color: #111827;
  margin-right: 8px;
}

.permission-action {
  color: #2563eb;
  font-family: monospace;
  background-color: #eff6ff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
}

.permission-desc {
  color: #6b7280;
  font-size: 12px;
}
</style>
