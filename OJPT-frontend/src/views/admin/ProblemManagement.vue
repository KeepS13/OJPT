<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Edit, Upload, Box } from '@element-plus/icons-vue'
import {
  archiveAdminProblem,
  getAdminProblemList,
  publishAdminProblem,
} from '@/api/admin'
import type {
  AdminProblemListItemVO,
  AdminProblemListParams,
  ProblemPublishStatus,
} from '@/types/admin'

const router = useRouter()

const loading = ref(false)
const problems = ref<AdminProblemListItemVO[]>([])
const total = ref(0)

const params = ref<AdminProblemListParams>({
  page: 1,
  size: 20,
  status: 'PUBLISHED',
})

const keywordInput = ref('')

const activeStatus = computed<ProblemPublishStatus>(
  () => (params.value.status ?? 'PUBLISHED') as ProblemPublishStatus,
)

const statusTabs: Array<{ key: ProblemPublishStatus; label: string }> = [
  { key: 'PUBLISHED', label: '已发布' },
  { key: 'DRAFT', label: '待审核（草稿）' },
  { key: 'ARCHIVED', label: '已归档' },
]

const statusTagType = (status: ProblemPublishStatus) => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'DRAFT') return 'warning'
  return 'info'
}

const statusLabel = (status: ProblemPublishStatus) => {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'DRAFT') return '草稿'
  return '已归档'
}

const loadProblems = async () => {
  loading.value = true
  try {
    const res = await getAdminProblemList(params.value)
    if (res.data && typeof res.data === 'object' && 'records' in res.data) {
      problems.value = (res.data as any).records ?? []
      total.value = (res.data as any).total ?? 0
    } else {
      problems.value = res.data as any
      total.value = problems.value.length
    }
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载题目列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  keywordInput.value = ''
  params.value = { page: 1, size: params.value.size ?? 20, status: activeStatus.value }
  loadProblems()
}

const onSearch = () => {
  params.value.page = 1
  params.value.keyword = keywordInput.value.trim() ? keywordInput.value.trim() : undefined
  loadProblems()
}

const onTabChange = (key: ProblemPublishStatus) => {
  params.value.status = key
  params.value.page = 1
  loadProblems()
}

const onPageChange = (page: number) => {
  params.value.page = page
  loadProblems()
}

const onSizeChange = (size: number) => {
  params.value.size = size
  params.value.page = 1
  loadProblems()
}

const goEdit = (row: AdminProblemListItemVO) => {
  router.push(`/admin/problems/${row.id}`)
}

const doPublish = async (row: AdminProblemListItemVO) => {
  try {
    loading.value = true
    await publishAdminProblem(row.id)
    ElMessage.success('发布成功')
    await loadProblems()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '发布失败')
  } finally {
    loading.value = false
  }
}

const doArchive = async (row: AdminProblemListItemVO) => {
  try {
    loading.value = true
    await archiveAdminProblem(row.id)
    ElMessage.success('归档成功')
    await loadProblems()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '归档失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => [params.value.page, params.value.size],
  () => {
    // 仅在分页参数变化时自动加载，过滤项变化由按钮触发
    loadProblems()
  }
)

onMounted(() => {
  loadProblems()
})
</script>

<template>
  <div class="admin-problem-page">
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="left">
          <el-input
            v-model="keywordInput"
            placeholder="按标题搜索"
            clearable
            style="max-width: 320px"
            @keyup.enter="onSearch"
          />
          <el-button :icon="Search" type="primary" @click="onSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
        <div class="right">
          <span class="meta">共 {{ total }} 条</span>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs :model-value="activeStatus" @tab-change="(k: string) => onTabChange(k as any)">
        <el-tab-pane v-for="t in statusTabs" :key="t.key" :label="t.label" :name="t.key" />
      </el-tabs>

      <el-table :data="problems" v-loading="loading" row-key="id" style="width: 100%">
        <el-table-column label="题号" width="90">
          <template #default="{ row }">
            <span v-if="row.problemNo != null">
              {{ 'P' + String(row.problemNo).padStart(4, '0') }}
            </span>
            <span v-else>--</span>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="标题" min-width="220" />

        <el-table-column label="难度" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.difficulty === 'EASY'" type="success">简单</el-tag>
            <el-tag v-else-if="row.difficulty === 'MEDIUM'" type="warning">中等</el-tag>
            <el-tag v-else type="danger">困难</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <template v-if="row.tags && row.tags.length">
              <el-tag v-for="t in row.tags" :key="t.id" type="info" style="margin-right: 6px">
                {{ t.name }}
              </el-tag>
            </template>
            <span v-else class="muted">--</span>
          </template>
        </el-table-column>

        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            <span class="muted">{{ row.updatedAt ?? '--' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button :icon="Edit" type="primary" link @click="goEdit(row)">编辑</el-button>

            <el-divider direction="vertical" />

            <el-button
              v-if="row.status === 'DRAFT'"
              :icon="Upload"
              type="success"
              link
              @click="doPublish(row)"
            >
              发布
            </el-button>

            <el-button
              v-if="row.status !== 'ARCHIVED'"
              :icon="Box"
              type="warning"
              link
              @click="doArchive(row)"
            >
              归档
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="params.page"
          v-model:page-size="params.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          background
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.admin-problem-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.toolbar-card :deep(.el-card__body) {
  padding: 12px 14px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar .left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta {
  color: #6b7280;
  font-size: 13px;
}

.muted {
  color: #6b7280;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}
</style>

