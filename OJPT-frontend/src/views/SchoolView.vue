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
  ElTabs,
  ElTabPane,
  ElCard,
  ElRow,
  ElCol,
  ElPagination,
} from 'element-plus'
import {
  getSchoolInfo,
  updateSchoolInfo,
  getDepartments,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  getSchoolClasses,
  getSchoolTeachers,
  addTeacher,
  removeTeacher,
  updateTeacher,
  getSchoolStudents,
  updateStudent,
  getSchoolStatisticsOverview,
  getDepartmentStatistics,
  getClassStatistics,
} from '@/api/school'
import type {
  SchoolVO,
  SchoolUpdateDTO,
  DepartmentVO,
  DepartmentCreateDTO,
  DepartmentUpdateDTO,
  ClassVO,
  TeacherVO,
  UserDetail,
  SchoolStatisticsOverview,
  DepartmentStatistics,
  ClassStatistics,
} from '@/types/school'
import type { UserUpdateDTO } from '@/api/user'

const loading = ref(false)
const activeTab = ref('overview')

// 学校信息
const schoolInfo = ref<SchoolVO | null>(null)
const showEditSchoolDialog = ref(false)
const schoolForm = ref<SchoolUpdateDTO>({ name: '', contact: '', status: 1 })
const schoolFormRef = ref()

// 统计数据
const statisticsOverview = ref<SchoolStatisticsOverview | null>(null)
const departmentStatistics = ref<DepartmentStatistics[]>([])
const classStatistics = ref<ClassStatistics[]>([])

// 院系管理
const departments = ref<DepartmentVO[]>([])
const deptParams = ref({ page: 1, size: 10 })
const totalDepts = ref(0)
const showCreateDeptDialog = ref(false)
const showEditDeptDialog = ref(false)
const selectedDept = ref<DepartmentVO | null>(null)
const deptForm = ref<DepartmentCreateDTO>({ name: '' })
const deptFormRef = ref()

// 班级管理
const classes = ref<ClassVO[]>([])
const classParams = ref({ page: 1, size: 10 })
const totalClasses = ref(0)

// 教师管理
const teachers = ref<TeacherVO[]>([])
const teacherParams = ref({ page: 1, size: 10 })
const totalTeachers = ref(0)
const showAddTeacherDialog = ref(false)
const addTeacherForm = ref({ userId: '' })
const showEditTeacherDialog = ref(false)
const selectedTeacher = ref<TeacherVO | null>(null)
const teacherForm = ref<UserUpdateDTO>({})

// 学员管理
const students = ref<UserDetail[]>([])
const studentParams = ref({ page: 1, size: 10 })
const totalStudents = ref(0)
const showEditStudentDialog = ref(false)
const selectedStudent = ref<UserDetail | null>(null)
const studentForm = ref<UserUpdateDTO>({})

// 加载学校信息
const loadSchoolInfo = async () => {
  try {
    loading.value = true
    const res = await getSchoolInfo()
    schoolInfo.value = res.data
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载学校信息失败')
  } finally {
    loading.value = false
  }
}

// 打开编辑学校弹窗
const handleEditSchool = () => {
  if (schoolInfo.value) {
    schoolForm.value = {
      name: schoolInfo.value.name,
      contact: schoolInfo.value.contact || '',
      status: schoolInfo.value.status,
    }
    showEditSchoolDialog.value = true
  }
}

// 提交更新学校信息
const submitUpdateSchool = async () => {
  if (!schoolFormRef.value) return
  await schoolFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        loading.value = true
        await updateSchoolInfo(schoolForm.value)
        ElMessage.success('更新成功')
        showEditSchoolDialog.value = false
        await loadSchoolInfo()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    loading.value = true
    const [overviewRes, deptRes, classRes] = await Promise.all([
      getSchoolStatisticsOverview(),
      getDepartmentStatistics(),
      getClassStatistics(),
    ])
    statisticsOverview.value = overviewRes.data
    departmentStatistics.value = deptRes.data
    classStatistics.value = classRes.data
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// 加载院系列表
const loadDepartments = async () => {
  try {
    loading.value = true
    const res = await getDepartments(deptParams.value)
    const data = res.data as { records?: DepartmentVO[]; total?: number } | DepartmentVO[]
    if (data && 'records' in data && Array.isArray(data.records)) {
      departments.value = data.records
      totalDepts.value = data.total ?? 0
    } else {
      departments.value = Array.isArray(data) ? data : []
      totalDepts.value = departments.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载院系列表失败')
  } finally {
    loading.value = false
  }
}

const handleDeptPageChange = (page: number) => {
  deptParams.value.page = page
  loadDepartments()
}
const handleDeptSizeChange = (size: number) => {
  deptParams.value.size = size
  deptParams.value.page = 1
  loadDepartments()
}

// 打开创建院系弹窗
const handleCreateDept = () => {
  deptForm.value = { name: '' }
  showCreateDeptDialog.value = true
}

// 提交创建院系
const submitCreateDept = async () => {
  if (!deptFormRef.value) return
  await deptFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        loading.value = true
        await createDepartment(deptForm.value)
        ElMessage.success('创建成功')
        showCreateDeptDialog.value = false
        await loadDepartments()
        await loadStatistics()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '创建失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 打开编辑院系弹窗
const handleEditDept = (dept: DepartmentVO) => {
  selectedDept.value = dept
  deptForm.value = { name: dept.name }
  showEditDeptDialog.value = true
}

// 提交更新院系
const submitUpdateDept = async () => {
  if (!deptFormRef.value || !selectedDept.value) return
  await deptFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        loading.value = true
        const payload: DepartmentUpdateDTO = { name: deptForm.value.name }
        await updateDepartment(selectedDept.value!.id, payload)
        ElMessage.success('更新成功')
        showEditDeptDialog.value = false
        await loadDepartments()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 删除院系
const handleDeleteDept = async (deptId: string) => {
  try {
    loading.value = true
    await deleteDepartment(deptId)
    ElMessage.success('删除成功')
    await loadDepartments()
    await loadStatistics()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
  } finally {
    loading.value = false
  }
}

// 加载班级列表
const loadClasses = async () => {
  try {
    loading.value = true
    const res = await getSchoolClasses(classParams.value)
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

// 加载教师列表
const loadTeachers = async () => {
  try {
    loading.value = true
    const res = await getSchoolTeachers(teacherParams.value)
    const data = res.data as { records?: TeacherVO[]; total?: number } | TeacherVO[]
    if (data && 'records' in data && Array.isArray(data.records)) {
      teachers.value = data.records
      totalTeachers.value = data.total ?? 0
    } else {
      teachers.value = Array.isArray(data) ? data : []
      totalTeachers.value = teachers.value.length
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
  loadTeachers()
}
const handleTeacherSizeChange = (size: number) => {
  teacherParams.value.size = size
  teacherParams.value.page = 1
  loadTeachers()
}

// 添加教师
const handleAddTeacher = () => {
  addTeacherForm.value = { userId: '' }
  showAddTeacherDialog.value = true
}

// 提交添加教师
const submitAddTeacher = async () => {
  try {
    loading.value = true
    await addTeacher({ userId: addTeacherForm.value.userId })
    ElMessage.success('添加成功')
    showAddTeacherDialog.value = false
    await loadTeachers()
    await loadStatistics()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '添加失败')
  } finally {
    loading.value = false
  }
}

// 打开编辑教师弹窗
const handleEditTeacher = (teacher: TeacherVO) => {
  selectedTeacher.value = teacher
  teacherForm.value = {
    email: teacher.email,
    phone: teacher.phone,
    studentNo: teacher.studentNo,
  }
  showEditTeacherDialog.value = true
}

// 提交更新教师
const submitUpdateTeacher = async () => {
  if (!selectedTeacher.value) return
  try {
    loading.value = true
    await updateTeacher(selectedTeacher.value.userId, teacherForm.value)
    ElMessage.success('更新成功')
    showEditTeacherDialog.value = false
    await loadTeachers()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 移除教师
const handleRemoveTeacher = async (teacherId: string) => {
  try {
    loading.value = true
    await removeTeacher(teacherId)
    ElMessage.success('移除成功')
    await loadTeachers()
    await loadStatistics()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '移除失败')
  } finally {
    loading.value = false
  }
}

// 加载学员列表
const loadStudents = async () => {
  try {
    loading.value = true
    const res = await getSchoolStudents(studentParams.value)
    const data = res.data as { records?: UserDetail[]; total?: number } | UserDetail[]
    if (data && 'records' in data && Array.isArray(data.records)) {
      students.value = data.records
      totalStudents.value = data.total ?? 0
    } else {
      students.value = Array.isArray(data) ? data : []
      totalStudents.value = students.value.length
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
  loadStudents()
}
const handleStudentSizeChange = (size: number) => {
  studentParams.value.size = size
  studentParams.value.page = 1
  loadStudents()
}

// 打开编辑学员弹窗
const handleEditStudent = (student: UserDetail) => {
  selectedStudent.value = student
  studentForm.value = {
    email: student.email,
    phone: student.phone,
    studentNo: student.studentNo,
  }
  showEditStudentDialog.value = true
}

// 提交更新学员
const submitUpdateStudent = async () => {
  if (!selectedStudent.value) return
  try {
    loading.value = true
    await updateStudent(selectedStudent.value.userId, studentForm.value)
    ElMessage.success('更新成功')
    showEditStudentDialog.value = false
    await loadStudents()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 格式化状态
const formatStatus = (status: number): { text: string; type: 'success' | 'warning' | 'danger' | 'info' } => {
  const map: Record<number, { text: string; type: 'success' | 'warning' | 'danger' | 'info' }> = {
    0: { text: '禁用', type: 'danger' },
    1: { text: '启用', type: 'success' },
    2: { text: '待认证', type: 'warning' },
  }
  return map[status] || { text: '未知', type: 'info' }
}

onMounted(() => {
  loadSchoolInfo()
  loadStatistics()
  loadDepartments()
  loadClasses()
  loadTeachers()
  loadStudents()
})
</script>

<template>
  <div class="school-view">
    <div class="school-header">
      <h1 class="school-title">校方管理</h1>
      <p class="school-subtitle">管理学校信息、院系、班级、教师和学员</p>
    </div>

    <div class="school-content">
      <el-tabs v-model="activeTab">
        <!-- 数据概览 -->
        <el-tab-pane label="数据概览" name="overview">
          <div class="section">
            <h2 class="section-title">学校信息</h2>
            <el-card v-if="schoolInfo" class="info-card">
              <div class="info-row">
                <span class="info-label">学校名称：</span>
                <span class="info-value">{{ schoolInfo.name }}</span>
              </div>
              <div class="info-row" v-if="schoolInfo.contact">
                <span class="info-label">联系方式：</span>
                <span class="info-value">{{ schoolInfo.contact }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">状态：</span>
                <el-tag :type="formatStatus(schoolInfo.status).type">
                  {{ formatStatus(schoolInfo.status).text }}
                </el-tag>
              </div>
              <div class="info-actions">
                <el-button type="primary" @click="handleEditSchool">编辑信息</el-button>
              </div>
            </el-card>

            <h2 class="section-title" style="margin-top: 32px">数据统计</h2>
            <el-row :gutter="20" v-if="statisticsOverview">
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-value">{{ statisticsOverview.statusCount.departments }}</div>
                  <div class="stat-label">院系数量</div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-value">{{ statisticsOverview.statusCount.classes }}</div>
                  <div class="stat-label">班级数量</div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-value">{{ statisticsOverview.statusCount.teachers }}</div>
                  <div class="stat-label">教师数量</div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card class="stat-card">
                  <div class="stat-value">{{ statisticsOverview.statusCount.students }}</div>
                  <div class="stat-label">学员数量</div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- 院系管理 -->
        <el-tab-pane label="院系管理" name="departments">
          <div class="section">
            <div class="section-header">
              <h2 class="section-title">院系列表</h2>
              <el-button type="primary" @click="handleCreateDept">创建院系</el-button>
            </div>
            <el-table :data="departments" v-loading="loading" style="width: 100%">
              <el-table-column prop="name" label="院系名称" />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="handleEditDept(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="handleDeleteDept(row.id)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="deptParams.page"
                v-model:page-size="deptParams.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="totalDepts"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handleDeptPageChange"
                @size-change="handleDeptSizeChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 班级管理 -->
        <el-tab-pane label="班级管理" name="classes">
          <div class="section">
            <h2 class="section-title">班级列表</h2>
            <el-table :data="classes" v-loading="loading" style="width: 100%">
              <el-table-column prop="name" label="班级名称" width="200" />
              <el-table-column prop="departmentName" label="院系" width="180" />
              <el-table-column prop="teacherName" label="班主任" width="120" />
              <el-table-column prop="year" label="届/年份" width="100" />
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

        <!-- 教师管理 -->
        <el-tab-pane label="教师管理" name="teachers">
          <div class="section">
            <div class="section-header">
              <h2 class="section-title">教师列表</h2>
              <el-button type="primary" @click="handleAddTeacher">添加教师</el-button>
            </div>
            <el-table :data="teachers" v-loading="loading" style="width: 100%">
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="email" label="邮箱" width="200" />
              <el-table-column prop="phone" label="手机号" width="120" />
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="handleEditTeacher(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="handleRemoveTeacher(row.userId)">
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
                :total="totalTeachers"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handleTeacherPageChange"
                @size-change="handleTeacherSizeChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 学员管理 -->
        <el-tab-pane label="学员管理" name="students">
          <div class="section">
            <h2 class="section-title">学员列表</h2>
            <el-table :data="students" v-loading="loading" style="width: 100%">
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="email" label="邮箱" width="200" />
              <el-table-column prop="phone" label="手机号" width="120" />
              <el-table-column prop="studentNo" label="学号" width="120" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" @click="handleEditStudent(row)">编辑</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="studentParams.page"
                v-model:page-size="studentParams.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="totalStudents"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handleStudentPageChange"
                @size-change="handleStudentSizeChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 编辑学校信息弹窗 -->
    <ElDialog v-model="showEditSchoolDialog" title="编辑学校信息" width="500px">
      <el-form ref="schoolFormRef" :model="schoolForm" label-width="100px">
        <el-form-item label="学校名称" prop="name" :rules="[{ required: true, message: '请输入学校名称' }]">
          <el-input v-model="schoolForm.name" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="schoolForm.contact" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
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

    <!-- 创建院系弹窗 -->
    <ElDialog v-model="showCreateDeptDialog" title="创建院系" width="500px">
      <el-form ref="deptFormRef" :model="deptForm" label-width="100px">
        <el-form-item label="院系名称" prop="name" :rules="[{ required: true, message: '请输入院系名称' }]">
          <el-input v-model="deptForm.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateDeptDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitCreateDept">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 编辑院系弹窗 -->
    <ElDialog v-model="showEditDeptDialog" title="编辑院系" width="500px">
      <el-form ref="deptFormRef" :model="deptForm" label-width="100px">
        <el-form-item label="院系名称" prop="name" :rules="[{ required: true, message: '请输入院系名称' }]">
          <el-input v-model="deptForm.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditDeptDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdateDept">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 添加教师弹窗 -->
    <ElDialog v-model="showAddTeacherDialog" title="添加教师" width="500px">
      <el-form :model="addTeacherForm" label-width="100px">
        <el-form-item label="用户ID">
          <el-input v-model="addTeacherForm.userId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddTeacherDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitAddTeacher">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 编辑教师弹窗 -->
    <ElDialog v-model="showEditTeacherDialog" title="编辑教师" width="500px">
      <el-form :model="teacherForm" label-width="100px">
        <el-form-item label="邮箱">
          <el-input v-model="teacherForm.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="teacherForm.phone" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="teacherForm.studentNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditTeacherDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdateTeacher">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 编辑学员弹窗 -->
    <ElDialog v-model="showEditStudentDialog" title="编辑学员" width="500px">
      <el-form :model="studentForm" label-width="100px">
        <el-form-item label="邮箱">
          <el-input v-model="studentForm.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="studentForm.phone" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="studentForm.studentNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditStudentDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUpdateStudent">确定</el-button>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.school-view {
  max-width: 1400px;
  margin: 0 auto;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.school-header {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.school-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.school-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.school-content {
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

.info-card {
  margin-bottom: 24px;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  width: 120px;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: #111827;
  flex: 1;
}

.info-actions {
  margin-top: 16px;
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
  .school-view {
    padding: 20px;
    border-radius: 8px;
  }
}
</style>

