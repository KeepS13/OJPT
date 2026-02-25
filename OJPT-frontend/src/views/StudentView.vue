<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElDialog, ElTable, ElTableColumn, ElButton, ElTag, ElPagination } from 'element-plus'
import { getMyClasses, getClassDetail, applyToClass, quitClass, getClassMembers } from '@/api/student'
import type { ClassVO, ClassMemberVO } from '@/types/student'

const loading = ref(false)
const classes = ref<ClassVO[]>([])
const classParams = ref({ page: 1, size: 10 })
const totalClasses = ref(0)
const selectedClass = ref<ClassVO | null>(null)
const showClassDetailDialog = ref(false)
const showMembersDialog = ref(false)
const members = ref<ClassMemberVO[]>([])
const membersClassId = ref<string | null>(null)
const membersParams = ref({ page: 1, size: 10 })
const totalMembers = ref(0)

// 加载班级列表
const loadClasses = async () => {
  try {
    loading.value = true
    const res = await getMyClasses(classParams.value)
    const data = res.data as { records?: ClassVO[]; total?: number } | ClassVO[]
    if (data && 'records' in data && Array.isArray(data.records)) {
      classes.value = data.records
      totalClasses.value = data.total ?? 0
    } else {
      classes.value = Array.isArray(data) ? data : []
      totalClasses.value = classes.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载班级列表失败')
  } finally {
    loading.value = false
  }
}

const handleClassPageChange = (page: number) => {
  classParams.value.page = page
  loadClasses()
}
const handleClassSizeChange = (size: number) => {
  classParams.value.size = size
  classParams.value.page = 1
  loadClasses()
}

// 查看班级详情
const handleViewDetail = async (classItem: ClassVO) => {
  try {
    loading.value = true
    const res = await getClassDetail(classItem.id)
    selectedClass.value = res.data
    showClassDetailDialog.value = true
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载班级详情失败')
  } finally {
    loading.value = false
  }
}

// 查看班级成员
const loadMembers = async () => {
  if (!membersClassId.value) return
  try {
    loading.value = true
    const res = await getClassMembers(membersClassId.value, membersParams.value)
    const data = res.data as { records?: ClassMemberVO[]; total?: number } | ClassMemberVO[]
    if (data && 'records' in data && Array.isArray(data.records)) {
      members.value = data.records
      totalMembers.value = data.total ?? 0
    } else {
      members.value = Array.isArray(data) ? data : []
      totalMembers.value = members.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载成员列表失败')
  } finally {
    loading.value = false
  }
}

const handleViewMembers = async (classId: string) => {
  membersClassId.value = classId
  membersParams.value.page = 1
  showMembersDialog.value = true
  await loadMembers()
}

const handleMembersPageChange = (page: number) => {
  membersParams.value.page = page
  loadMembers()
}
const handleMembersSizeChange = (size: number) => {
  membersParams.value.size = size
  membersParams.value.page = 1
  loadMembers()
}

// 申请加入班级
const handleApply = async (classId: string) => {
  try {
    loading.value = true
    await applyToClass(classId)
    ElMessage.success('申请成功，等待审核')
    await loadClasses()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '申请失败')
  } finally {
    loading.value = false
  }
}

// 退出班级
const handleQuit = async (classId: string) => {
  try {
    loading.value = true
    await quitClass(classId)
    ElMessage.success('退出成功')
    await loadClasses()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '退出失败')
  } finally {
    loading.value = false
  }
}

// 格式化加入状态
const formatJoinStatus = (status?: string): { text: string; type: 'success' | 'warning' | 'danger' | 'info' } => {
  const map: Record<string, { text: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
    PENDING: { text: '待审核', type: 'warning' },
    APPROVED: { text: '已通过', type: 'success' },
    REJECTED: { text: '已拒绝', type: 'danger' },
  }
  return map[status || ''] || { text: '未知', type: 'info' }
}

// 格式化加入类型
const formatJoinType = (type?: string) => {
  return type === 'APPLY' ? '申请加入' : type === 'INVITE' ? '邀请加入' : '未知'
}

onMounted(() => {
  loadClasses()
})
</script>

<template>
  <div class="student-view">
    <div class="student-header">
      <h1 class="student-title">学员中心</h1>
      <p class="student-subtitle">管理您加入的班级</p>
    </div>

    <div class="student-content">
      <div class="section">
        <h2 class="section-title">我的班级</h2>
        <el-table :data="classes" v-loading="loading" style="width: 100%">
          <el-table-column prop="name" label="班级名称" width="200" />
          <el-table-column prop="departmentName" label="院系" width="180" />
          <el-table-column prop="schoolName" label="学校" width="180" />
          <el-table-column prop="teacherName" label="班主任" width="120" />
          <el-table-column prop="year" label="届/年份" width="100" />
          <el-table-column label="加入状态" width="120">
            <template #default="{ row }">
              <el-tag :type="formatJoinStatus(row.joinStatus).type">
                {{ formatJoinStatus(row.joinStatus).text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="加入类型" width="120">
            <template #default="{ row }">
              {{ formatJoinType(row.joinType) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleViewDetail(row)">查看详情</el-button>
              <el-button
                v-if="row.joinStatus === 'APPROVED'"
                size="small"
                type="primary"
                @click="handleViewMembers(row.id)"
              >
                查看成员
              </el-button>
              <el-button
                v-if="row.joinStatus === 'APPROVED'"
                size="small"
                type="danger"
                @click="handleQuit(row.id)"
              >
                退出班级
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="classParams.page"
            v-model:page-size="classParams.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="totalClasses"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handleClassPageChange"
            @size-change="handleClassSizeChange"
          />
        </div>
      </div>
    </div>

    <!-- 班级详情弹窗 -->
    <ElDialog v-model="showClassDetailDialog" title="班级详情" width="600px">
      <div v-if="selectedClass" class="class-detail">
        <div class="detail-row">
          <span class="detail-label">班级名称：</span>
          <span class="detail-value">{{ selectedClass.name }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">院系：</span>
          <span class="detail-value">{{ selectedClass.departmentName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">学校：</span>
          <span class="detail-value">{{ selectedClass.schoolName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">班主任：</span>
          <span class="detail-value">{{ selectedClass.teacherName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">届/年份：</span>
          <span class="detail-value">{{ selectedClass.year || '未设置' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">班级类型：</span>
          <span class="detail-value">{{ selectedClass.merk || '未设置' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">加入状态：</span>
          <el-tag :type="formatJoinStatus(selectedClass.joinStatus).type">
            {{ formatJoinStatus(selectedClass.joinStatus).text }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">加入类型：</span>
          <span class="detail-value">{{ formatJoinType(selectedClass.joinType) }}</span>
        </div>
        <div class="detail-row" v-if="selectedClass.joinAt">
          <span class="detail-label">加入时间：</span>
          <span class="detail-value">{{ selectedClass.joinAt }}</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showClassDetailDialog = false">关闭</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 成员列表弹窗 -->
    <ElDialog v-model="showMembersDialog" title="班级成员" width="700px">
      <el-table :data="members" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column label="加入类型" width="120">
          <template #default="{ row }">
            {{ formatJoinType(row.joinType) }}
          </template>
        </el-table-column>
        <el-table-column prop="joinAt" label="加入时间" width="180" />
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="membersParams.page"
          v-model:page-size="membersParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalMembers"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleMembersPageChange"
          @size-change="handleMembersSizeChange"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showMembersDialog = false">关闭</el-button>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.student-view {
  max-width: 1200px;
  margin: 0 auto;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.student-header {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.student-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.student-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.student-content {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.section {
  width: 100%;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 16px 0;
}

.class-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.detail-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  width: 120px;
  flex-shrink: 0;
}

.detail-value {
  font-size: 14px;
  color: #111827;
  flex: 1;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .student-view {
    padding: 20px;
    border-radius: 8px;
  }
}
</style>



