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
  ElPagination,
} from 'element-plus'
import {
  getMyManagedClasses,
  createClass,
  updateClass,
  deleteClass,
  getClassStudents,
  getClassApplications,
  approveApplication,
  rejectApplication,
  removeStudent,
  getClassTeachers,
  addTeacherToClass,
  removeTeacherFromClass,
} from '@/api/teacher'
import type {
  ClassVO,
  ClassMemberVO,
  ClassCreateDTO,
  ClassUpdateDTO,
  ApplicationVO,
  TeacherVO,
} from '@/types/teacher'

const loading = ref(false)
const classes = ref<ClassVO[]>([])
const classParams = ref({ page: 1, size: 10 })
const totalClasses = ref(0)
const activeTab = ref('classes')

// 班级管理相关
const showCreateClassDialog = ref(false)
const showEditClassDialog = ref(false)
const selectedClass = ref<ClassVO | null>(null)
const classForm = ref<ClassCreateDTO>({
  departmentId: '',
  name: '',
  year: '',
  teacherId: undefined,
  merk: '',
})
const classFormRef = ref()

// 学员管理相关
const selectedClassId = ref<string | null>(null)
const students = ref<ClassMemberVO[]>([])
const studentParams = ref({ page: 1, size: 10 })
const totalClassStudents = ref(0)
const applications = ref<ApplicationVO[]>([])
const applicationParams = ref({ page: 1, size: 10 })
const totalApplications = ref(0)
const showReviewDialog = ref(false)
const reviewForm = ref({ reviewComment: '' })
const currentApplication = ref<ApplicationVO | null>(null)
const isApproving = ref(false)

// 教师管理相关
const teachers = ref<TeacherVO[]>([])
const teacherParams = ref({ page: 1, size: 10 })
const totalClassTeachers = ref(0)
const showAddTeacherDialog = ref(false)
const addTeacherForm = ref({ teacherId: '', role: '助教' })

// 加载班级列表
const loadClasses = async () => {
  try {
    loading.value = true
    const res = await getMyManagedClasses(classParams.value)
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

// 打开创建班级弹窗
const handleCreateClass = () => {
  classForm.value = {
    departmentId: '',
    name: '',
    year: '',
    teacherId: undefined,
    merk: '',
  }
  showCreateClassDialog.value = true
}

// 提交创建班级
const submitCreateClass = async () => {
  if (!classFormRef.value) return
  await classFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        loading.value = true
        await createClass(classForm.value)
        ElMessage.success('创建成功')
        showCreateClassDialog.value = false
        await loadClasses()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '创建失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 打开编辑班级弹窗
const handleEditClass = (classItem: ClassVO) => {
  selectedClass.value = classItem
  classForm.value = {
    departmentId: classItem.departmentId,
    name: classItem.name,
    year: classItem.year || '',
    teacherId: classItem.teacherId,
    merk: classItem.merk || '',
  }
  showEditClassDialog.value = true
}

// 提交更新班级
const submitUpdateClass = async () => {
  if (!classFormRef.value || !selectedClass.value) return
  await classFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        loading.value = true
        const payload: ClassUpdateDTO = {
          name: classForm.value.name,
          year: classForm.value.year,
          teacherId: classForm.value.teacherId,
          merk: classForm.value.merk,
        }
        await updateClass(selectedClass.value!.id, payload)
        ElMessage.success('更新成功')
        showEditClassDialog.value = false
        await loadClasses()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 删除班级
const handleDeleteClass = async (classId: string) => {
  try {
    loading.value = true
    await deleteClass(classId)
    ElMessage.success('删除成功')
    await loadClasses()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
  } finally {
    loading.value = false
  }
}

// 加载班级学员
const loadClassStudents = async (classId: string, resetPage = false) => {
  selectedClassId.value = classId
  activeTab.value = 'students'
  if (resetPage) {
    studentParams.value.page = 1
  }
  try {
    loading.value = true
    const res = await getClassStudents(classId, studentParams.value)
    const data = res.data as { records?: ClassMemberVO[]; total?: number } | ClassMemberVO[]
    if (data && 'records' in data && Array.isArray(data.records)) {
      students.value = data.records
      totalClassStudents.value = data.total ?? 0
    } else {
      students.value = Array.isArray(data) ? data : []
      totalClassStudents.value = students.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载学员列表失败')
  } finally {
    loading.value = false
  }
}

const handleStudentPageChange = (page: number) => {
  studentParams.value.page = page
  if (selectedClassId.value) loadClassStudents(selectedClassId.value)
}
const handleStudentSizeChange = (size: number) => {
  studentParams.value.size = size
  studentParams.value.page = 1
  if (selectedClassId.value) loadClassStudents(selectedClassId.value)
}

// 加载申请列表
const loadApplications = async (classId: string, resetPage = false) => {
  selectedClassId.value = classId
  activeTab.value = 'applications'
  if (resetPage) {
    applicationParams.value.page = 1
  }
  try {
    loading.value = true
    const res = await getClassApplications(classId, applicationParams.value)
    const data = res.data as { records?: ApplicationVO[]; total?: number } | ApplicationVO[]
    if (data && 'records' in data && Array.isArray(data.records)) {
      applications.value = data.records
      totalApplications.value = data.total ?? 0
    } else {
      applications.value = Array.isArray(data) ? data : []
      totalApplications.value = applications.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载申请列表失败')
  } finally {
    loading.value = false
  }
}

const handleApplicationPageChange = (page: number) => {
  applicationParams.value.page = page
  if (selectedClassId.value) loadApplications(selectedClassId.value)
}
const handleApplicationSizeChange = (size: number) => {
  applicationParams.value.size = size
  applicationParams.value.page = 1
  if (selectedClassId.value) loadApplications(selectedClassId.value)
}

// 打开审核弹窗
const handleReview = (application: ApplicationVO, approve: boolean) => {
  currentApplication.value = application
  isApproving.value = approve
  reviewForm.value.reviewComment = ''
  showReviewDialog.value = true
}

// 提交审核
const submitReview = async () => {
  if (!currentApplication.value || !selectedClassId.value) return
  try {
    loading.value = true
    if (isApproving.value) {
      await approveApplication(selectedClassId.value, currentApplication.value.id, {
        reviewComment: reviewForm.value.reviewComment,
      })
      ElMessage.success('批准成功')
    } else {
      await rejectApplication(selectedClassId.value, currentApplication.value.id, {
        reviewComment: reviewForm.value.reviewComment,
      })
      ElMessage.success('拒绝成功')
    }
    showReviewDialog.value = false
    await loadApplications(selectedClassId.value, false)
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 移除学员
const handleRemoveStudent = async (studentId: string) => {
  if (!selectedClassId.value) return
  try {
    loading.value = true
    await removeStudent(selectedClassId.value, studentId)
    ElMessage.success('移除成功')
    await loadClassStudents(selectedClassId.value)
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '移除失败')
  } finally {
    loading.value = false
  }
}

// 加载班级教师
const loadClassTeachers = async (classId: string, resetPage = false) => {
  selectedClassId.value = classId
  activeTab.value = 'teachers'
  if (resetPage) {
    teacherParams.value.page = 1
  }
  try {
    loading.value = true
    const res = await getClassTeachers(classId, teacherParams.value)
    const data = res.data as { records?: TeacherVO[]; total?: number } | TeacherVO[]
    if (data && 'records' in data && Array.isArray(data.records)) {
      teachers.value = data.records
      totalClassTeachers.value = data.total ?? 0
    } else {
      teachers.value = Array.isArray(data) ? data : []
      totalClassTeachers.value = teachers.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载教师列表失败')
  } finally {
    loading.value = false
  }
}

const handleTeacherPageChange = (page: number) => {
  teacherParams.value.page = page
  if (selectedClassId.value) loadClassTeachers(selectedClassId.value)
}
const handleTeacherSizeChange = (size: number) => {
  teacherParams.value.size = size
  teacherParams.value.page = 1
  if (selectedClassId.value) loadClassTeachers(selectedClassId.value)
}

// 添加教师
const handleAddTeacher = (classId: string) => {
  selectedClassId.value = classId
  addTeacherForm.value = { teacherId: '', role: '助教' }
  showAddTeacherDialog.value = true
}

// 提交添加教师
const submitAddTeacher = async () => {
  if (!selectedClassId.value) return
  try {
    loading.value = true
    await addTeacherToClass(selectedClassId.value, addTeacherForm.value.teacherId, addTeacherForm.value.role)
    ElMessage.success('添加成功')
    showAddTeacherDialog.value = false
    await loadClassTeachers(selectedClassId.value)
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '添加失败')
  } finally {
    loading.value = false
  }
}

// 移除教师
const handleRemoveTeacher = async (teacherId: string) => {
  if (!selectedClassId.value) return
  try {
    loading.value = true
    await removeTeacherFromClass(selectedClassId.value, teacherId)
    ElMessage.success('移除成功')
    await loadClassTeachers(selectedClassId.value)
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '移除失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadClasses()
})
</script>

<template>
  <div class="teacher-view">
    <div class="teacher-header">
      <h1 class="teacher-title">教师后台</h1>
      <p class="teacher-subtitle">管理您负责的班级、学员和教师</p>
    </div>

    <div class="teacher-content">
      <el-tabs v-model="activeTab">
        <!-- 班级管理 -->
        <el-tab-pane label="班级管理" name="classes">
          <div class="section">
            <div class="section-header">
              <h2 class="section-title">我的班级</h2>
              <el-button type="primary" @click="handleCreateClass">创建班级</el-button>
            </div>
            <el-table :data="classes" v-loading="loading" style="width: 100%">
              <el-table-column prop="name" label="班级名称" width="200" />
              <el-table-column prop="departmentName" label="院系" width="180" />
              <el-table-column prop="schoolName" label="学校" width="180" />
              <el-table-column prop="teacherName" label="班主任" width="120" />
              <el-table-column prop="year" label="届/年份" width="100" />
              <el-table-column label="操作" width="400" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="handleEditClass(row)">编辑</el-button>
                  <el-button size="small" type="primary" @click="loadClassStudents(row.id, true)">
                    学员管理
                  </el-button>
                  <el-button size="small" type="warning" @click="loadApplications(row.id, true)">
                    申请审核
                  </el-button>
                  <el-button size="small" type="info" @click="loadClassTeachers(row.id, true)">
                    教师管理
                  </el-button>
                  <el-button size="small" type="danger" @click="handleDeleteClass(row.id)">
                    删除
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
        </el-tab-pane>

        <!-- 学员管理 -->
        <el-tab-pane label="学员管理" name="students">
          <div class="section">
            <h2 class="section-title">班级学员</h2>
            <el-table :data="students" v-loading="loading" style="width: 100%">
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="email" label="邮箱" width="200" />
              <el-table-column prop="studentNo" label="学号" width="120" />
              <el-table-column prop="joinAt" label="加入时间" width="180" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" type="danger" @click="handleRemoveStudent(row.userId)">
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="studentParams.page"
                v-model:page-size="studentParams.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="totalClassStudents"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handleStudentPageChange"
                @size-change="handleStudentSizeChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 申请审核 -->
        <el-tab-pane label="申请审核" name="applications">
          <div class="section">
            <h2 class="section-title">待审核申请</h2>
            <el-table :data="applications" v-loading="loading" style="width: 100%">
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="email" label="邮箱" width="200" />
              <el-table-column prop="studentNo" label="学号" width="120" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.joinStatus === 'PENDING' ? 'warning' : 'info'">
                    {{ row.joinStatus === 'PENDING' ? '待审核' : row.joinStatus }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.joinStatus === 'PENDING'"
                    size="small"
                    type="success"
                    @click="handleReview(row, true)"
                  >
                    批准
                  </el-button>
                  <el-button
                    v-if="row.joinStatus === 'PENDING'"
                    size="small"
                    type="danger"
                    @click="handleReview(row, false)"
                  >
                    拒绝
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="applicationParams.page"
                v-model:page-size="applicationParams.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="totalApplications"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handleApplicationPageChange"
                @size-change="handleApplicationSizeChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 教师管理 -->
        <el-tab-pane label="教师管理" name="teachers">
          <div class="section">
            <div class="section-header">
              <h2 class="section-title">班级教师</h2>
              <el-button
                v-if="selectedClassId"
                type="primary"
                @click="handleAddTeacher(selectedClassId)"
              >
                添加教师
              </el-button>
            </div>
            <el-table :data="teachers" v-loading="loading" style="width: 100%">
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="email" label="邮箱" width="200" />
              <el-table-column prop="role" label="角色" width="120" />
              <el-table-column prop="createdAt" label="添加时间" width="180" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.role !== '班主任'"
                    size="small"
                    type="danger"
                    @click="handleRemoveTeacher(row.teacherId)"
                  >
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="teacherParams.page"
                v-model:page-size="teacherParams.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="totalClassTeachers"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handleTeacherPageChange"
                @size-change="handleTeacherSizeChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 创建班级弹窗 -->
    <ElDialog v-model="showCreateClassDialog" title="创建班级" width="500px">
      <el-form ref="classFormRef" :model="classForm" label-width="100px">
        <el-form-item label="院系ID" prop="departmentId" :rules="[{ required: true, message: '请输入院系ID' }]">
          <el-input v-model="classForm.departmentId" />
        </el-form-item>
        <el-form-item label="班级名称" prop="name" :rules="[{ required: true, message: '请输入班级名称' }]">
          <el-input v-model="classForm.name" />
        </el-form-item>
        <el-form-item label="届/年份" prop="year">
          <el-input v-model="classForm.year" />
        </el-form-item>
        <el-form-item label="班主任ID" prop="teacherId">
          <el-input v-model.number="classForm.teacherId" type="number" />
        </el-form-item>
        <el-form-item label="班级类型" prop="merk">
          <el-input v-model="classForm.merk" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateClassDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitCreateClass">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 编辑班级弹窗 -->
    <ElDialog v-model="showEditClassDialog" title="编辑班级" width="500px">
      <el-form ref="classFormRef" :model="classForm" label-width="100px">
        <el-form-item label="班级名称" prop="name" :rules="[{ required: true, message: '请输入班级名称' }]">
          <el-input v-model="classForm.name" />
        </el-form-item>
        <el-form-item label="届/年份" prop="year">
          <el-input v-model="classForm.year" />
        </el-form-item>
        <el-form-item label="班主任ID" prop="teacherId">
          <el-input v-model="classForm.teacherId" />
        </el-form-item>
        <el-form-item label="班级类型" prop="merk">
          <el-input v-model="classForm.merk" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditClassDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdateClass">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 审核弹窗 -->
    <ElDialog v-model="showReviewDialog" :title="isApproving ? '批准申请' : '拒绝申请'" width="500px">
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showReviewDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitReview">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 添加教师弹窗 -->
    <ElDialog v-model="showAddTeacherDialog" title="添加教师" width="500px">
      <el-form :model="addTeacherForm" label-width="100px">
        <el-form-item label="教师ID">
          <el-input v-model="addTeacherForm.teacherId" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addTeacherForm.role">
            <el-option label="助教" value="助教" />
            <el-option label="任课教师" value="任课教师" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddTeacherDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitAddTeacher">确定</el-button>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.teacher-view {
  max-width: 1400px;
  margin: 0 auto;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.teacher-header {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.teacher-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.teacher-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.teacher-content {
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
  margin: 0;
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
  .teacher-view {
    padding: 20px;
    border-radius: 8px;
  }
}
</style>


