<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentUserSubmissionRecords, type UserSubmissionRecord } from '@/api/user'

const loading = ref(false)
const records = ref<UserSubmissionRecord[]>([])
const total = ref(0)
const params = ref({ page: 1, size: 10 })
const codeDialogVisible = ref(false)
const selectedRecord = ref<UserSubmissionRecord | null>(null)

const statusTagTypeMap: Record<string, string> = {
  AC: 'success',
  WA: 'danger',
  CE: 'warning',
  TLE: 'warning',
  MLE: 'warning',
  RE: 'danger',
  SYSTEM_ERROR: 'info',
  QUEUED: 'info',
  RUNNING: 'primary',
}

const emptyText = computed(() => (loading.value ? '加载中...' : '还没有提交记录'))

const loadRecords = async () => {
  try {
    loading.value = true
    const res = await getCurrentUserSubmissionRecords(params.value)
    records.value = res.data.records ?? []
    total.value = res.data.total ?? 0
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载解题记录失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  params.value.page = page
  loadRecords()
}

const handleSizeChange = (size: number) => {
  params.value.size = size
  params.value.page = 1
  loadRecords()
}

const openCodeDialog = (record: UserSubmissionRecord) => {
  selectedRecord.value = record
  codeDialogVisible.value = true
}

onMounted(() => {
  loadRecords()
})
</script>

<template>
  <div class="submission-records-view">
    <div class="page-header">
      <h1 class="page-title">解题记录</h1>
      <p class="page-subtitle">查看你历史提交过的题目、结果和代码内容。</p>
    </div>

    <el-card shadow="never" class="records-card">
      <el-table
        v-loading="loading"
        :data="records"
        style="width: 100%"
        :empty-text="emptyText"
      >
        <el-table-column label="题目" min-width="220">
          <template #default="{ row }">
            <div class="problem-cell">
              <span v-if="row.problemNo != null" class="problem-no">
                {{ 'P' + String(row.problemNo).padStart(4, '0') }}
              </span>
              <span class="problem-title">{{ row.problemTitle || '未知题目' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="language" label="语言" width="110" />

        <el-table-column label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="(statusTagTypeMap[row.status] || 'info') as any">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="耗时 / 内存" width="160">
          <template #default="{ row }">
            <span>{{ row.timeMs ?? '--' }} ms / {{ row.memoryKb ?? '--' }} KB</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="提交时间" width="180" />

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openCodeDialog(row)">查看代码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="params.page"
          v-model:page-size="params.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="codeDialogVisible" title="提交代码" width="760px">
      <div v-if="selectedRecord" class="code-dialog">
        <div class="code-dialog__meta">
          <span>{{ selectedRecord.problemTitle || '未知题目' }}</span>
          <span>{{ selectedRecord.language }}</span>
          <span>{{ selectedRecord.status }}</span>
        </div>
        <pre class="code-block"><code>{{ selectedRecord.sourceCode }}</code></pre>
        <p v-if="selectedRecord.compileMessage" class="judge-message">
          编译信息：{{ selectedRecord.compileMessage }}
        </p>
        <p v-if="selectedRecord.judgeMessage" class="judge-message">
          判题信息：{{ selectedRecord.judgeMessage }}
        </p>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.submission-records-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  color: #111827;
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

.records-card {
  border-radius: 18px;
}

.problem-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.problem-no {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  padding: 2px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  color: #374151;
}

.problem-title {
  color: #111827;
  font-weight: 500;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.code-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.code-dialog__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #6b7280;
  font-size: 13px;
}

.code-block {
  margin: 0;
  padding: 14px;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 12px;
  overflow: auto;
  white-space: pre-wrap;
  font-size: 12px;
  line-height: 1.6;
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.judge-message {
  margin: 0;
  font-size: 13px;
  color: #475569;
}
</style>
