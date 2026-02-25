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
  ElPagination,
  ElPopconfirm,
} from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import {
  getSchoolList,
  createSchool,
  updateSchool,
  deleteSchool,
  updateSchoolStatus,
  certifySchool,
  uncertifySchool,
} from '@/api/admin'
import type {
  SchoolVO,
  SchoolListParams,
  SchoolCreateDTO,
} from '@/types/admin'

const loading = ref(false)

// 学校列表数据
const schools = ref<SchoolVO[]>([])
const schoolParams = ref<SchoolListParams>({ page: 1, size: 10 })
const totalSchools = ref(0)

// 创建/编辑学校相关
const showCreateSchoolDialog = ref(false)
const showEditSchoolDialog = ref(false)
const selectedSchool = ref<SchoolVO | null>(null)
const schoolForm = ref<SchoolCreateDTO>({ name: '', contact: '', status: 1 })

// 状态配置
const statusOptions = [
  { value: 1, label: '启用', type: 'success' },
  { value: 0, label: '禁用', type: 'danger' },
  { value: 2, label: '待认证', type: 'warning' },
]

const getStatusConfig = (status: number) => {
  return statusOptions.find(s => s.value === status) || { label: '未知', type: 'info' }
}

// 加载学校列表
const loadSchools = async () => {
  try {
    loading.value = true
    const res = await getSchoolList(schoolParams.value)
    // 处理新的分页响应格式
    if (res.data && 'records' in res.data) {
      schools.value = res.data.records
      totalSchools.value = res.data.total
    } else {
      // 兼容旧格式
      schools.value = res.data as unknown as SchoolVO[]
      totalSchools.value = schools.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载学校列表失败')
  } finally {
    loading.value = false
  }
}

// 重置筛选
const resetFilters = () => {
  schoolParams.value = { page: 1, size: 10 }
  loadSchools()
}

// 分页变化处理
const handlePageChange = (page: number) => {
  schoolParams.value.page = page
  loadSchools()
}

const handleSizeChange = (size: number) => {
  schoolParams.value.size = size
  schoolParams.value.page = 1
  loadSchools()
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

onMounted(() => {
  loadSchools()
})
</script>

<template>
  <div class="school-management">
    <!-- 筛选和操作栏 -->
    <div class="filter-section">
      <div class="filter-row">
        <el-input
          v-model="schoolParams.keyword"
          placeholder="搜索学校名称"
          style="width: 240px"
          clearable
          @keyup.enter="loadSchools"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select 
          v-model="schoolParams.status" 
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
        <el-button type="primary" @click="loadSchools">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="resetFilters">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
        <div style="flex: 1"></div>
        <el-button type="primary" @click="handleCreateSchool">
          <el-icon><Plus /></el-icon>
          创建学校
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-table :data="schools" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="学校名称" width="200" />
        <el-table-column prop="contact" label="联系方式" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusConfig(row.status).type as any">
              {{ getStatusConfig(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="departmentCount" label="院系数" width="80" />
        <el-table-column prop="classCount" label="班级数" width="80" />
        <el-table-column prop="teacherCount" label="教师数" width="80" />
        <el-table-column prop="studentCount" label="学员数" width="80" />
        <el-table-column label="操作" width="320" fixed="right">
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
            <el-popconfirm
              title="确定要删除该学校吗？"
              @confirm="handleDeleteSchool(row.id)"
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
          v-model:current-page="schoolParams.page"
          v-model:page-size="schoolParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalSchools"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 创建学校弹窗 -->
    <el-dialog v-model="showCreateSchoolDialog" title="创建学校" width="500px">
      <el-form :model="schoolForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="schoolForm.name" placeholder="请输入学校名称" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="schoolForm.contact" placeholder="请输入联系方式" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="schoolForm.status" style="width: 100%">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateSchoolDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitCreateSchool">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑学校弹窗 -->
    <el-dialog v-model="showEditSchoolDialog" title="编辑学校" width="500px">
      <el-form :model="schoolForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="schoolForm.name" placeholder="请输入学校名称" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="schoolForm.contact" placeholder="请输入联系方式" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="schoolForm.status" style="width: 100%">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditSchoolDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitUpdateSchool">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.school-management {
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
