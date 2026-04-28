<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProblemList } from '@/api/problem'
import type { PageResponse } from '@/api/base'

type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
type ProblemStatus = 'UNSOLVED' | 'ATTEMPTED' | 'SOLVED'

interface ProblemTagVO {
  id: string
  name: string
  type?: string | null
}

interface ProblemListItemVO {
  id: string
  problemNo: number
  title: string
  difficulty: Difficulty
  acceptanceRate?: number | null
  tags?: ProblemTagVO[] | null
  status?: ProblemStatus | null
}

const route = useRoute()
const router = useRouter()

const getRouteKeyword = () => {
  const keyword = route.query.keyword
  return typeof keyword === 'string' ? keyword : ''
}

const difficulties: { label: string; value: Difficulty }[] = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' },
]

const statusOptions: { label: string; value: ProblemStatus | 'ALL' }[] = [
  { label: '全部', value: 'ALL' },
  { label: '未开始', value: 'UNSOLVED' },
  { label: '尝试中', value: 'ATTEMPTED' },
  { label: '已通过', value: 'SOLVED' },
]

const problems = ref<ProblemListItemVO[]>([])
const loading = ref(false)

const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchKeyword = ref(getRouteKeyword())
const activeDifficulty = ref<Difficulty | 'ALL'>('ALL')
const activeStatus = ref<ProblemStatus | 'ALL'>('ALL')
const activeTagId = ref<string | number | 'ALL'>('ALL')
const tagOptions = ref<ProblemTagVO[]>([])

let searchTimer: ReturnType<typeof setTimeout> | null = null
let skipNextFilterFetch = false
let skipNextSearchFetch = false

const currentPageCount = computed(() => problems.value.length)
const solvedCount = computed(() => problems.value.filter((p) => p.status === 'SOLVED').length)
const easyCount = computed(() => problems.value.filter((p) => p.difficulty === 'EASY').length)
const mediumCount = computed(() => problems.value.filter((p) => p.difficulty === 'MEDIUM').length)
const hardCount = computed(() => problems.value.filter((p) => p.difficulty === 'HARD').length)

const selectedDifficultyLabel = computed(() => {
  if (activeDifficulty.value === 'ALL') return ''
  return difficulties.find((item) => item.value === activeDifficulty.value)?.label ?? ''
})

const selectedStatusLabel = computed(() => {
  if (activeStatus.value === 'ALL') return ''
  return statusOptions.find((item) => item.value === activeStatus.value)?.label ?? ''
})

const selectedTagLabel = computed(() => {
  if (activeTagId.value === 'ALL') return ''
  return tagOptions.value.find((tag) => String(tag.id) === String(activeTagId.value))?.name ?? ''
})

const hasActiveFilters = computed(
  () =>
    searchKeyword.value.trim() ||
    activeDifficulty.value !== 'ALL' ||
    activeStatus.value !== 'ALL' ||
    activeTagId.value !== 'ALL',
)

const activeFilterSummary = computed(() => {
  const parts: string[] = []
  if (selectedDifficultyLabel.value) parts.push(`难度：${selectedDifficultyLabel.value}`)
  if (selectedStatusLabel.value) parts.push(`状态：${selectedStatusLabel.value}`)
  if (selectedTagLabel.value) parts.push(`标签：${selectedTagLabel.value}`)
  if (searchKeyword.value.trim()) parts.push(`搜索：${searchKeyword.value.trim()}`)
  return parts.join(' · ')
})

const currentPageDifficultySummary = computed(
  () => `当前页：简单 ${easyCount.value} / 中等 ${mediumCount.value} / 困难 ${hardCount.value}`,
)

const getDifficultyClass = (difficulty: Difficulty) => {
  if (difficulty === 'EASY') return 'difficulty-badge difficulty-badge--easy'
  if (difficulty === 'MEDIUM') return 'difficulty-badge difficulty-badge--medium'
  return 'difficulty-badge difficulty-badge--hard'
}

const getDifficultyLabel = (difficulty: Difficulty) => {
  if (difficulty === 'EASY') return '简单'
  if (difficulty === 'MEDIUM') return '中等'
  return '困难'
}

const getStatusClass = (status?: ProblemStatus | null) => {
  if (status === 'SOLVED') return 'status-badge status-badge--solved'
  if (status === 'ATTEMPTED') return 'status-badge status-badge--attempted'
  return 'status-badge status-badge--unsolved'
}

const getStatusLabel = (status?: ProblemStatus | null) => {
  if (status === 'SOLVED') return '已通过'
  if (status === 'ATTEMPTED') return '尝试中'
  return '未开始'
}

const clearSearchTimer = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
}

const buildQueryParams = () => {
  const params: Record<string, unknown> = {
    page: page.value,
    size: pageSize.value,
  }
  if (searchKeyword.value.trim()) {
    params.keyword = searchKeyword.value.trim()
  }
  if (activeDifficulty.value !== 'ALL') {
    params.difficulty = activeDifficulty.value
  }
  if (activeStatus.value !== 'ALL') {
    params.status = activeStatus.value
  }
  if (activeTagId.value !== 'ALL') {
    params.tagId = activeTagId.value
  }
  return params
}

const fetchProblems = async () => {
  loading.value = true
  try {
    const res = await getProblemList(buildQueryParams())
    const payload = res.data as PageResponse<ProblemListItemVO> | { data: PageResponse<ProblemListItemVO> }
    const pageData =
      (payload && 'records' in payload ? (payload as PageResponse<ProblemListItemVO>) : (payload as any).data) ??
      null

    problems.value = pageData?.records ?? []
    total.value = pageData?.total ?? 0
    mergeTagOptions(problems.value)
  } catch (e) {
    ElMessage.error('加载题库失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const mergeTagOptions = (records: ProblemListItemVO[]) => {
  const tagMap = new Map<string, ProblemTagVO>()
  tagOptions.value.forEach((tag) => tagMap.set(String(tag.id), tag))
  records.forEach((problem) => {
    const tags = problem.tags ?? []
    tags.forEach((tag) => {
      tagMap.set(String(tag.id), tag)
    })
  })
  tagOptions.value = Array.from(tagMap.values())
}

const setDifficulty = (difficulty: Difficulty | 'ALL') => {
  activeDifficulty.value = difficulty
  page.value = 1
}

const setStatus = (status: ProblemStatus | 'ALL') => {
  activeStatus.value = status
  page.value = 1
}

const setTag = (tagId: string | number | 'ALL') => {
  activeTagId.value = tagId
  page.value = 1
}

const clearKeyword = () => {
  clearSearchTimer()
  skipNextSearchFetch = searchKeyword.value !== ''
  skipNextFilterFetch = page.value !== 1
  searchKeyword.value = ''
  page.value = 1
  fetchProblems()
}

const clearFilters = () => {
  clearSearchTimer()
  skipNextSearchFetch = searchKeyword.value !== ''
  skipNextFilterFetch =
    page.value !== 1 ||
    activeDifficulty.value !== 'ALL' ||
    activeStatus.value !== 'ALL' ||
    activeTagId.value !== 'ALL'
  searchKeyword.value = ''
  activeDifficulty.value = 'ALL'
  activeStatus.value = 'ALL'
  activeTagId.value = 'ALL'
  page.value = 1
  fetchProblems()
}

const goProblem = (problemNo: number) => {
  router.push(`/problems/${problemNo}`)
}

onMounted(fetchProblems)

onBeforeUnmount(() => {
  clearSearchTimer()
})

watch([page, pageSize, activeDifficulty, activeStatus, activeTagId], () => {
  if (skipNextFilterFetch) {
    skipNextFilterFetch = false
    return
  }
  fetchProblems()
})

watch(searchKeyword, () => {
  if (skipNextSearchFetch) {
    skipNextSearchFetch = false
    return
  }
  clearSearchTimer()
  searchTimer = setTimeout(() => {
    skipNextFilterFetch = page.value !== 1
    page.value = 1
    fetchProblems()
  }, 300)
})

watch(
  () => route.query.keyword,
  () => {
    const keyword = getRouteKeyword()
    if (keyword === searchKeyword.value) {
      return
    }
    clearSearchTimer()
    skipNextSearchFetch = true
    searchKeyword.value = keyword
    skipNextFilterFetch = page.value !== 1
    page.value = 1
    fetchProblems()
  },
)
</script>

<template>
  <main class="problemset-page">
    <section class="problemset-header">
      <div class="header-title-group">
        <h1 class="title">题库</h1>
        <p class="subtitle">按难度、状态和已加载标签筛选题目。</p>
      </div>
      <div class="summary-bar" aria-label="题库摘要">
        <span>共 {{ total }} 题</span>
        <span>本页 {{ currentPageCount }} 题</span>
        <span>已通过 {{ solvedCount }} / {{ currentPageCount }}</span>
      </div>
    </section>

    <section class="problemset-filters" aria-label="题目筛选">
      <div class="filter-toolbar">
        <div class="filter-group">
          <span class="filter-label">难度</span>
          <button
            class="chip"
            :class="{ 'chip--active': activeDifficulty === 'ALL' }"
            type="button"
            @click="setDifficulty('ALL')"
          >
            全部
          </button>
          <button
            v-for="d in difficulties"
            :key="d.value"
            class="chip"
            :class="[
              `chip--${d.value.toLowerCase()}`,
              { 'chip--active': activeDifficulty === d.value },
            ]"
            type="button"
            @click="setDifficulty(d.value)"
          >
            {{ d.label }}
          </button>
        </div>

        <div class="filter-group">
          <span class="filter-label">状态</span>
          <button
            v-for="s in statusOptions"
            :key="s.value"
            class="chip chip--ghost"
            :class="{ 'chip--active': activeStatus === s.value }"
            type="button"
            @click="setStatus(s.value)"
          >
            {{ s.label }}
          </button>
        </div>

        <div class="search-box">
          <span class="search-icon" aria-hidden="true">⌕</span>
          <input
            v-model="searchKeyword"
            data-testid="problem-search-input"
            type="text"
            class="search-input"
            placeholder="搜索题目 / 标签 / 题号"
          />
          <button
            v-if="searchKeyword"
            data-testid="problem-search-clear"
            class="search-clear"
            type="button"
            aria-label="清空搜索"
            @click="clearKeyword"
          >
            ×
          </button>
        </div>

        <button
          data-testid="clear-filters-button"
          class="clear-filter-button"
          type="button"
          :disabled="!hasActiveFilters"
          @click="clearFilters"
        >
          清空筛选
        </button>
      </div>

      <div v-if="tagOptions.length" class="tag-filter-row">
        <span class="filter-label">已加载标签</span>
        <div class="tag-chip-list">
          <button
            class="chip chip--ghost"
            :class="{ 'chip--active': activeTagId === 'ALL' }"
            type="button"
            @click="setTag('ALL')"
          >
            全部
          </button>
          <button
            v-for="tag in tagOptions"
            :key="tag.id"
            class="chip chip--ghost"
            :class="{ 'chip--active': String(activeTagId) === String(tag.id) }"
            type="button"
            @click="setTag(tag.id)"
          >
            {{ tag.name }}
          </button>
        </div>
      </div>
    </section>

    <section class="problemset-table">
      <div class="table-header">
        <div>
          <span class="table-header-title">题目列表 · 共 {{ total }} 题</span>
          <span v-if="activeFilterSummary" class="table-filter-summary">{{ activeFilterSummary }}</span>
        </div>
        <span class="table-header-meta">{{ currentPageDifficultySummary }}</span>
      </div>

      <table class="problem-table">
        <thead>
          <tr>
            <th class="col-status">状态</th>
            <th class="col-id">题号</th>
            <th class="col-title">标题</th>
            <th class="col-difficulty">难度</th>
            <th class="col-acceptance">通过率</th>
            <th class="col-tags">标签</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6" class="table-message">正在加载题库...</td>
          </tr>
          <tr v-else-if="!problems.length">
            <td colspan="6" class="empty-cell">
              <div class="empty-state">
                <strong>暂无匹配题目</strong>
                <span>调整关键词或筛选条件后再试。</span>
                <button
                  data-testid="empty-clear-filters"
                  class="clear-filter-button"
                  type="button"
                  @click="clearFilters"
                >
                  清空筛选
                </button>
              </div>
            </td>
          </tr>
          <tr
            v-for="p in problems"
            v-else
            :key="p.id"
            class="problem-row"
            @click="goProblem(p.problemNo)"
          >
            <td class="col-status">
              <span :class="getStatusClass(p.status || null)">
                {{ getStatusLabel(p.status || null) }}
              </span>
            </td>
            <td class="col-id">
              {{ 'P' + String(p.problemNo).padStart(4, '0') }}
            </td>
            <td class="col-title">
              <div class="title-main">
                <RouterLink
                  :to="`/problems/${String(p.problemNo)}`"
                  class="problem-link"
                  @click.stop
                >
                  {{ p.title }}
                </RouterLink>
                <div v-if="p.tags?.length" class="title-tags-mobile">
                  <span
                    v-for="tag in (p.tags || []).slice(0, 3)"
                    :key="tag.id"
                    class="tag-pill"
                  >
                    {{ tag.name }}
                  </span>
                  <span v-if="(p.tags || []).length > 3" class="tag-pill tag-pill--more">
                    +{{ (p.tags || []).length - 3 }}
                  </span>
                </div>
              </div>
            </td>
            <td class="col-difficulty">
              <span :class="getDifficultyClass(p.difficulty)">
                {{ getDifficultyLabel(p.difficulty) }}
              </span>
            </td>
            <td class="col-acceptance">
              {{
                p.acceptanceRate != null
                  ? `${Number(p.acceptanceRate).toFixed(1)}%`
                  : '--'
              }}
            </td>
            <td class="col-tags">
              <span
                v-for="tag in (p.tags || []).slice(0, 3)"
                :key="tag.id"
                class="tag-pill"
              >
                {{ tag.name }}
              </span>
              <span v-if="(p.tags || []).length > 3" class="tag-pill tag-pill--more">
                +{{ (p.tags || []).length - 3 }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination-bar">
        <div class="pagination-info">
          当前第 {{ page }} 页，每页 {{ pageSize }} 条
        </div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          background
        />
      </div>
    </section>
  </main>
</template>

<style scoped>
.problemset-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px 0 32px;
}

.problemset-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.header-title-group {
  min-width: 0;
}

.title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  letter-spacing: 0;
}

.subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.summary-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  color: #374151;
  font-size: 13px;
}

.summary-bar span {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #ffffff;
  padding: 5px 9px;
  white-space: nowrap;
}

.problemset-filters {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background-color: #ffffff;
}

.filter-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 16px;
}

.filter-group,
.tag-filter-row,
.tag-chip-list {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-filter-row {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f3f4f6;
}

.tag-chip-list {
  min-width: 0;
}

.filter-label {
  font-size: 13px;
  color: #6b7280;
  white-space: nowrap;
}

.chip {
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background-color: #ffffff;
  color: #374151;
  padding: 4px 10px;
  font-size: 12px;
  line-height: 1.35;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease;
}

.chip:hover {
  border-color: #9ca3af;
  background-color: #f9fafb;
}

.chip--ghost {
  background-color: #ffffff;
}

.chip--easy {
  color: #15803d;
  border-color: #bbf7d0;
}

.chip--medium {
  color: #b45309;
  border-color: #fde68a;
}

.chip--hard {
  color: #b91c1c;
  border-color: #fecaca;
}

.chip--active,
.chip--active:hover {
  border-color: #2563eb;
  background-color: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}

.search-box {
  position: relative;
  margin-left: auto;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  font-size: 13px;
  pointer-events: none;
}

.search-input {
  width: 260px;
  height: 32px;
  padding: 6px 32px 6px 28px;
  border-radius: 7px;
  border: 1px solid #d1d5db;
  font-size: 13px;
  outline: none;
}

.search-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.search-input::placeholder {
  color: #9ca3af;
}

.search-clear {
  position: absolute;
  right: 7px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 999px;
  background: #f3f4f6;
  color: #6b7280;
  cursor: pointer;
  line-height: 18px;
}

.clear-filter-button {
  height: 32px;
  border: 1px solid #d1d5db;
  border-radius: 7px;
  background: #ffffff;
  color: #374151;
  padding: 0 11px;
  font-size: 12px;
  cursor: pointer;
}

.clear-filter-button:hover:not(:disabled) {
  border-color: #9ca3af;
  background: #f9fafb;
}

.clear-filter-button:disabled {
  color: #9ca3af;
  cursor: not-allowed;
}

.problemset-table {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  overflow: hidden;
}

.table-header {
  min-height: 48px;
  padding: 10px 14px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.table-header-title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.table-filter-summary,
.table-header-meta {
  display: block;
  margin-top: 3px;
  font-size: 12px;
  color: #6b7280;
}

.table-header-meta {
  margin-top: 0;
  text-align: right;
  white-space: nowrap;
}

.problem-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.problem-table thead {
  background-color: #f9fafb;
}

.problem-table th {
  padding: 8px 12px;
  font-weight: 600;
  color: #6b7280;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.problem-table td {
  padding: 8px 12px;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: middle;
}

.problem-row {
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.problem-row:hover {
  background-color: #f9fafb;
}

.col-status {
  width: 92px;
}

.col-id {
  width: 86px;
  color: #6b7280;
  font-variant-numeric: tabular-nums;
}

.col-title {
  width: 38%;
}

.col-difficulty {
  width: 92px;
}

.col-acceptance {
  width: 92px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.col-tags {
  min-width: 160px;
}

.problem-link {
  color: #111827;
  text-decoration: none;
  font-weight: 600;
}

.problem-link:hover {
  color: #2563eb;
}

.title-tags-mobile {
  display: none;
}

.difficulty-badge,
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.difficulty-badge {
  padding: 2px 8px;
}

.difficulty-badge--easy {
  background-color: #f0fdf4;
  color: #15803d;
}

.difficulty-badge--medium {
  background-color: #fffbeb;
  color: #b45309;
}

.difficulty-badge--hard {
  background-color: #fef2f2;
  color: #b91c1c;
}

.status-badge {
  min-width: 58px;
  padding: 2px 8px;
}

.status-badge--solved {
  background-color: #ecfdf5;
  color: #047857;
}

.status-badge--attempted {
  background-color: #fff7ed;
  color: #c2410c;
}

.status-badge--unsolved {
  background-color: #f3f4f6;
  color: #6b7280;
}

.tag-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 7px;
  border-radius: 999px;
  background-color: #f3f4f6;
  color: #4b5563;
  font-size: 11px;
  margin: 1px 4px 1px 0;
  white-space: nowrap;
}

.tag-pill--more {
  color: #2563eb;
  background-color: #eff6ff;
}

.pagination-bar {
  padding: 10px 14px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination-info {
  font-size: 12px;
  color: #6b7280;
}

.table-message,
.empty-cell {
  text-align: center;
  padding: 22px 14px;
  color: #6b7280;
  font-size: 13px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.empty-state strong {
  color: #111827;
  font-size: 14px;
}

@media (max-width: 960px) {
  .problemset-page {
    padding: 16px 0 28px;
  }

  .problemset-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-bar {
    justify-content: flex-start;
  }

  .filter-toolbar {
    align-items: stretch;
  }

  .search-box {
    width: 100%;
    margin-left: 0;
    order: -1;
  }

  .search-input {
    width: 100%;
  }

  .col-tags {
    display: none;
  }

  .title-tags-mobile {
    display: block;
    margin-top: 5px;
  }

  .table-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-header-meta {
    text-align: left;
    white-space: normal;
  }

  .pagination-bar {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .problem-table th,
  .problem-table td {
    padding: 8px;
  }

  .col-id {
    width: 70px;
  }

  .col-difficulty {
    width: 76px;
  }

  .col-acceptance {
    display: none;
  }
}
</style>
