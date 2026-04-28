<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, DocumentChecked, RefreshRight } from '@element-plus/icons-vue'
import {
  addTagToAdminProblem,
  getAdminProblemDetail,
  getAdminProblemTestCases,
  getAdminTags,
  removeTagFromAdminProblem,
  replaceAdminProblemTestCases,
  updateAdminProblem,
} from '@/api/admin'
import { renderMarkdown } from '@/utils/markdown'
import type { ProblemTestCaseVO, ProblemUpdateDTO, TagVO } from '@/types/admin'

type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

interface AdminProblemDetail {
  id: string
  problemNo?: number | null
  title: string
  difficulty: Difficulty
  status?: string | null
  statementMd?: string | null
  timeLimitMs?: number | null
  memoryLimitKb?: number | null
  tags?: TagVO[] | null
}

const route = useRoute()
const router = useRouter()

const problemId = computed(() => String(route.params.problemId ?? ''))

const loading = ref(false)
const saving = ref(false)

const detail = ref<AdminProblemDetail | null>(null)
const allTags = ref<TagVO[]>([])
const sampleCases = ref<ProblemTestCaseVO[]>([])
const hiddenCases = ref<ProblemTestCaseVO[]>([])

const form = ref<ProblemUpdateDTO>({
  title: '',
  difficulty: 'EASY',
  statementMd: '',
  timeLimitMs: 1000,
  memoryLimitKb: 256000,
})

const originalTagIds = ref<string[]>([])
const selectedTagIds = ref<string[]>([])

const previewHtml = computed(() => (form.value.statementMd ? renderMarkdown(form.value.statementMd) : ''))

const createEmptyCase = (caseType: 'SAMPLE' | 'HIDDEN', sortOrder: number): ProblemTestCaseVO => ({
  caseType,
  sortOrder,
  inputText: '',
  expectedOutput: '',
  explanation: '',
})

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getAdminProblemDetail(problemId.value)
    const data = res.data as any
    detail.value = data as AdminProblemDetail

    form.value = {
      title: data?.title ?? '',
      difficulty: (data?.difficulty ?? 'EASY') as Difficulty,
      statementMd: data?.statementMd ?? '',
      timeLimitMs: data?.timeLimitMs ?? 1000,
      memoryLimitKb: data?.memoryLimitKb ?? 256000,
    }

    const tagIds = Array.isArray(data?.tags) ? (data.tags as TagVO[]).map((t) => String(t.id)) : []
    originalTagIds.value = [...tagIds]
    selectedTagIds.value = [...tagIds]
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载题目详情失败')
  } finally {
    loading.value = false
  }
}

const loadTags = async () => {
  try {
    const res = await getAdminTags()
    allTags.value = res.data ?? []
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载标签列表失败')
  }
}

const loadTestCases = async () => {
  try {
    const res = await getAdminProblemTestCases(problemId.value)
    const cases = res.data ?? []
    sampleCases.value = cases
      .filter((item) => item.caseType === 'SAMPLE')
      .sort((a, b) => a.sortOrder - b.sortOrder)
    hiddenCases.value = cases
      .filter((item) => item.caseType === 'HIDDEN')
      .sort((a, b) => a.sortOrder - b.sortOrder)
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载测试用例失败')
  }
}

const addCase = (caseType: 'SAMPLE' | 'HIDDEN') => {
  const target = caseType === 'SAMPLE' ? sampleCases.value : hiddenCases.value
  target.push(createEmptyCase(caseType, target.length + 1))
}

const removeCase = (caseType: 'SAMPLE' | 'HIDDEN', index: number) => {
  const target = caseType === 'SAMPLE' ? sampleCases.value : hiddenCases.value
  target.splice(index, 1)
  target.forEach((item, idx) => {
    item.sortOrder = idx + 1
  })
}

const save = async () => {
  if (!problemId.value) return
  if (saving.value) return

  saving.value = true
  try {
    await updateAdminProblem(problemId.value, form.value)

    const current = new Set(selectedTagIds.value.map(String))
    const original = new Set(originalTagIds.value.map(String))

    const toAdd = Array.from(current).filter((id) => !original.has(id))
    const toRemove = Array.from(original).filter((id) => !current.has(id))

    for (const id of toAdd) {
      await addTagToAdminProblem(problemId.value, id)
    }
    for (const id of toRemove) {
      await removeTagFromAdminProblem(problemId.value, id)
    }

    const normalizedCases: ProblemTestCaseVO[] = [
      ...sampleCases.value.map((item, index) => ({
        ...item,
        caseType: 'SAMPLE' as const,
        sortOrder: index + 1,
        explanation: item.explanation || '',
      })),
      ...hiddenCases.value.map((item, index) => ({
        ...item,
        caseType: 'HIDDEN' as const,
        sortOrder: index + 1,
        explanation: item.explanation || '',
      })),
    ]

    await replaceAdminProblemTestCases(problemId.value, {
      cases: normalizedCases,
    })

    originalTagIds.value = Array.from(current)
    ElMessage.success('保存成功')

    await Promise.all([loadDetail(), loadTestCases()])
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const back = () => {
  router.push('/admin/problems')
}

onMounted(async () => {
  await Promise.all([loadDetail(), loadTags(), loadTestCases()])
})
</script>

<template>
  <div class="problem-edit-page">
    <div class="topbar">
      <el-button :icon="ArrowLeft" @click="back">返回列表</el-button>
      <div class="title">
        <div class="name">
          <span v-if="detail?.problemNo != null" class="no">
            {{ 'P' + String(detail.problemNo).padStart(4, '0') }}
          </span>
          <span>{{ detail?.title ?? '题目编辑' }}</span>
        </div>
        <div class="sub">
          <span class="muted">ID: {{ problemId }}</span>
          <span v-if="detail?.status" class="muted">状态：{{ detail.status }}</span>
        </div>
      </div>
      <div class="actions">
        <el-button :icon="RefreshRight" :loading="loading" @click="loadDetail">刷新</el-button>
        <el-button type="primary" :icon="DocumentChecked" :loading="saving" @click="save">
          保存
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-form label-position="top" class="form">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入题目标题" />
        </el-form-item>

        <div class="grid2">
          <el-form-item label="难度">
            <el-select v-model="form.difficulty" placeholder="选择难度">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>

          <el-form-item label="标签">
            <el-select
              v-model="selectedTagIds"
              placeholder="选择标签"
              multiple
              filterable
              clearable
            >
              <el-option v-for="t in allTags" :key="t.id" :label="t.name" :value="String(t.id)" />
            </el-select>
          </el-form-item>
        </div>

        <div class="grid2">
          <el-form-item label="时间限制（ms）">
            <el-input v-model.number="form.timeLimitMs" type="number" />
          </el-form-item>
          <el-form-item label="内存限制（KB）">
            <el-input v-model.number="form.memoryLimitKb" type="number" />
          </el-form-item>
        </div>

        <el-form-item label="题面（Markdown）">
          <div class="md-split">
            <div class="editor-pane">
              <div class="pane-header">编辑</div>
              <el-input
                v-model="form.statementMd"
                type="textarea"
                :rows="18"
                placeholder="请输入 Markdown 题面"
              />
            </div>
            <div class="preview-pane">
              <div class="pane-header">预览</div>
              <div v-if="previewHtml" class="markdown-body" v-html="previewHtml" />
              <div v-else class="markdown-empty">
                在左侧输入 Markdown，右侧会实时预览效果
              </div>
            </div>
          </div>
        </el-form-item>

        <div class="case-block">
          <div class="case-block__header">
            <div>
              <div class="case-block__title">样例测试用例</div>
              <div class="case-block__desc">公开展示给用户，做题页只读显示</div>
            </div>
            <el-button size="small" @click="addCase('SAMPLE')">新增样例</el-button>
          </div>

          <div v-if="sampleCases.length" class="case-list">
            <div v-for="(item, index) in sampleCases" :key="`sample-${index}`" class="case-card">
              <div class="case-card__top">
                <span class="case-card__name">Sample {{ index + 1 }}</span>
                <el-button text type="danger" @click="removeCase('SAMPLE', index)">删除</el-button>
              </div>
              <div class="grid2">
                <el-form-item label="输入">
                  <el-input v-model="item.inputText" type="textarea" :rows="4" />
                </el-form-item>
                <el-form-item label="输出">
                  <el-input v-model="item.expectedOutput" type="textarea" :rows="4" />
                </el-form-item>
              </div>
              <el-form-item label="说明">
                <el-input v-model="item.explanation" placeholder="可选：补充该样例说明" />
              </el-form-item>
            </div>
          </div>
          <div v-else class="case-empty">暂无样例测试用例</div>
        </div>

        <div class="case-block">
          <div class="case-block__header">
            <div>
              <div class="case-block__title">隐藏测试用例</div>
              <div class="case-block__desc">仅用于提交判题，不在做题页展示</div>
            </div>
            <el-button size="small" @click="addCase('HIDDEN')">新增隐藏用例</el-button>
          </div>

          <div v-if="hiddenCases.length" class="case-list">
            <div v-for="(item, index) in hiddenCases" :key="`hidden-${index}`" class="case-card">
              <div class="case-card__top">
                <span class="case-card__name">Hidden {{ index + 1 }}</span>
                <el-button text type="danger" @click="removeCase('HIDDEN', index)">删除</el-button>
              </div>
              <div class="grid2">
                <el-form-item label="输入">
                  <el-input v-model="item.inputText" type="textarea" :rows="4" />
                </el-form-item>
                <el-form-item label="输出">
                  <el-input v-model="item.expectedOutput" type="textarea" :rows="4" />
                </el-form-item>
              </div>
              <el-form-item label="说明">
                <el-input v-model="item.explanation" placeholder="可选：内部备注" />
              </el-form-item>
            </div>
          </div>
          <div v-else class="case-empty">暂无隐藏测试用例</div>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.problem-edit-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.topbar {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
}

.title .name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.no {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  padding: 2px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  color: #374151;
}

.sub {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
}

.muted {
  color: #6b7280;
}

.actions {
  display: flex;
  gap: 10px;
}

.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.md-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: stretch;
  width: 100%;
}

.editor-pane,
.preview-pane {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background-color: #fafafa;
  display: flex;
  flex-direction: column;
  min-height: 260px;
}

.pane-header {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #4b5563;
  border-bottom: 1px solid #e5e7eb;
  background-color: #f3f4f6;
}

.editor-pane :deep(textarea) {
  min-height: 260px;
}

.preview-pane .markdown-body {
  padding: 10px 12px;
  overflow: auto;
  max-height: 480px;
}

.markdown-empty {
  flex: 1;
  padding: 10px 12px;
  font-size: 13px;
  color: #9ca3af;
  display: flex;
  align-items: center;
}

.case-block {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
  background: #fcfcfd;
}

.case-block + .case-block {
  margin-top: 14px;
}

.case-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.case-block__title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.case-block__desc {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.case-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.case-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
}

.case-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.case-card__name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}

.case-empty {
  font-size: 13px;
  color: #9ca3af;
  padding: 6px 0 2px;
}

.markdown-body {
  font-size: 13px;
  color: #374151;
  line-height: 1.7;
}

:deep(.markdown-body > h1:first-child) {
  display: none;
}

:deep(.markdown-body p) {
  margin: 0 0 8px 0;
}

:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 20px;
  margin: 0 0 8px 0;
}

:deep(.markdown-body > ul) {
  list-style: none;
  padding: 8px 14px;
  margin: 10px 0 14px;
  border-left: 4px solid #d4d4d8;
  background-color: #f9fafb;
  border-radius: 4px;
}

:deep(.markdown-body code) {
  background-color: #f3f4f6;
  padding: 0 4px;
  border-radius: 4px;
}

:deep(.markdown-body pre) {
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  background-color: #f9fafb;
  color: #111827;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  white-space: pre-wrap;
  margin: 0 0 8px 0;
  overflow-x: auto;
}

:deep(.markdown-body pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  font-size: inherit;
  color: inherit;
}

@media (max-width: 1024px) {
  .md-split,
  .grid2 {
    grid-template-columns: 1fr;
  }

  .topbar {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .actions {
    width: 100%;
  }
}
</style>
