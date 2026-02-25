<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'

type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
type ProblemStatus = 'UNSOLVED' | 'ATTEMPTED' | 'SOLVED'

interface ProblemListItem {
  id: number
  title: string
  difficulty: Difficulty
  acceptanceRate: number
  tags: string[]
  status: ProblemStatus
}

const difficulties: { label: string; value: Difficulty }[] = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' },
]

const statusOptions: { label: string; value: ProblemStatus | 'ALL' }[] = [
  { label: '全部', value: 'ALL' },
  { label: '未做', value: 'UNSOLVED' },
  { label: '尝试中', value: 'ATTEMPTED' },
  { label: '已通过', value: 'SOLVED' },
]

const mockProblems = ref<ProblemListItem[]>([
  {
    id: 1,
    title: '两数之和',
    difficulty: 'EASY',
    acceptanceRate: 77.1,
    tags: ['数组', '哈希表'],
    status: 'SOLVED',
  },
  {
    id: 2,
    title: '两数相加',
    difficulty: 'MEDIUM',
    acceptanceRate: 55.1,
    tags: ['链表', '数学'],
    status: 'ATTEMPTED',
  },
  {
    id: 3,
    title: '无重复字符的最长子串',
    difficulty: 'MEDIUM',
    acceptanceRate: 46.5,
    tags: ['字符串', '滑动窗口'],
    status: 'UNSOLVED',
  },
  {
    id: 4,
    title: '寻找两个正序数组的中位数',
    difficulty: 'HARD',
    acceptanceRate: 42.0,
    tags: ['数组', '二分查找'],
    status: 'UNSOLVED',
  },
])

const page = ref(1)
const pageSize = ref(20)
const total = ref(4224)

const searchKeyword = ref('')
const activeDifficulty = ref<Difficulty | 'ALL'>('ALL')
const activeStatus = ref<ProblemStatus | 'ALL'>('ALL')
const orderBy = ref<'DEFAULT' | 'ID' | 'DIFFICULTY' | 'ACCEPTANCE'>('DEFAULT')

const getDifficultyClass = (difficulty: Difficulty) => {
  if (difficulty === 'EASY') return 'difficulty-badge difficulty-badge--easy'
  if (difficulty === 'MEDIUM') return 'difficulty-badge difficulty-badge--medium'
  return 'difficulty-badge difficulty-badge--hard'
}

const getStatusDotClass = (status: ProblemStatus) => {
  if (status === 'SOLVED') return 'status-dot status-dot--solved'
  if (status === 'ATTEMPTED') return 'status-dot status-dot--attempted'
  return 'status-dot status-dot--unsolved'
}
</script>

<template>
  <main class="problemset-page">
    <section class="problemset-header">
      <h1 class="title">题库</h1>
      <p class="subtitle">
        精选算法与数据结构题目，支持标签筛选、难度分级与做题进度统计，帮助你系统性练习。
      </p>
      <div class="stats">
        <div class="stat-item">
          <span class="stat-label">已通过</span>
          <span class="stat-value">0</span>
          <span class="stat-extra">/ 4224</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">简单</span>
          <span class="stat-pill stat-pill--easy">0 / 1200</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">中等</span>
          <span class="stat-pill stat-pill--medium">0 / 2000</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">困难</span>
          <span class="stat-pill stat-pill--hard">0 / 1024</span>
        </div>
      </div>
    </section>

    <section class="problemset-filters">
      <div class="filters-left">
        <div class="filter-group">
          <span class="filter-label">难度</span>
          <button
            class="chip"
            :class="{ 'chip--active': activeDifficulty === 'ALL' }"
            type="button"
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
          >
            {{ s.label }}
          </button>
        </div>
      </div>

      <div class="filters-right">
        <div class="search-box">
          <input
            v-model="searchKeyword"
            type="text"
            class="search-input"
            placeholder="搜索题目 / 标签 / 题号"
          />
        </div>
        <div class="sort-select">
          <label for="orderBy">排序：</label>
          <select id="orderBy" v-model="orderBy">
            <option value="DEFAULT">推荐</option>
            <option value="ID">题号</option>
            <option value="DIFFICULTY">难度</option>
            <option value="ACCEPTANCE">通过率</option>
          </select>
        </div>
      </div>
    </section>

    <section class="problemset-table">
      <div class="table-header">
        <span class="table-header-title">全部题目</span>
        <span class="table-header-meta">共 {{ total }} 题</span>
      </div>

      <table class="problem-table">
        <thead>
          <tr>
            <th class="col-status"></th>
            <th class="col-id">题号</th>
            <th class="col-title">标题</th>
            <th class="col-difficulty">难度</th>
            <th class="col-acceptance">通过率</th>
            <th class="col-tags">标签</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in mockProblems" :key="p.id" class="problem-row">
            <td class="col-status">
              <span :class="getStatusDotClass(p.status)" />
            </td>
            <td class="col-id">
              {{ p.id.toString().padStart(4, '0') }}
            </td>
            <td class="col-title">
              <div class="title-main">
                <RouterLink :to="`/problems/${p.id}`" class="problem-link">
                  {{ p.title }}
                </RouterLink>
              </div>
            </td>
            <td class="col-difficulty">
              <span :class="getDifficultyClass(p.difficulty)">
                {{
                  p.difficulty === 'EASY'
                    ? '简单'
                    : p.difficulty === 'MEDIUM'
                      ? '中等'
                      : '困难'
                }}
              </span>
            </td>
            <td class="col-acceptance">
              {{ p.acceptanceRate.toFixed(1) }}%
            </td>
            <td class="col-tags">
              <span v-for="tag in p.tags" :key="tag" class="tag-pill">
                {{ tag }}
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
  max-width: 1120px;
  margin: 0 auto;
  padding: 32px 0 40px;
}

.problemset-header {
  margin-bottom: 24px;
}

.title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 6px 0;
  letter-spacing: -0.5px;
}

.subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.stats {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #4b5563;
}

.stat-label {
  color: #6b7280;
}

.stat-value {
  font-weight: 600;
  font-size: 14px;
}

.stat-extra {
  color: #9ca3af;
}

.stat-pill {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.stat-pill--easy {
  background-color: #ecfdf3;
  color: #16a34a;
}

.stat-pill--medium {
  background-color: #fffbeb;
  color: #d97706;
}

.stat-pill--hard {
  background-color: #fef2f2;
  color: #dc2626;
}

.problemset-filters {
  margin-bottom: 16px;
  padding: 12px 16px;
  border-radius: 10px;
  background-color: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.filters-left {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 24px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 13px;
  color: #6b7280;
}

.chip {
  border-radius: 999px;
  border: 1px solid transparent;
  background-color: #e5e7eb;
  color: #374151;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip--ghost {
  background-color: transparent;
  border-color: #e5e7eb;
}

.chip--easy {
  background-color: #ecfdf3;
  color: #16a34a;
}

.chip--medium {
  background-color: #fffbeb;
  color: #d97706;
}

.chip--hard {
  background-color: #fef2f2;
  color: #dc2626;
}

.chip--active {
  border-color: #2563eb;
  background-color: #2563eb;
  color: #ffffff;
}

.filters-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  position: relative;
}

.search-input {
  width: 220px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  font-size: 13px;
  outline: none;
}

.search-input::placeholder {
  color: #9ca3af;
}

.sort-select {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
}

.sort-select select {
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  outline: none;
  background-color: #ffffff;
}

.problemset-table {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  overflow: hidden;
}

.table-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.table-header-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.table-header-meta {
  font-size: 12px;
  color: #9ca3af;
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
  padding: 10px 16px;
  font-weight: 500;
  color: #6b7280;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.problem-table td {
  padding: 10px 16px;
  border-bottom: 1px solid #f3f4f6;
}

.problem-row:hover {
  background-color: #f9fafb;
}

.col-status {
  width: 40px;
}

.col-id {
  width: 80px;
  color: #6b7280;
}

.col-title {
  width: 40%;
}

.col-difficulty {
  width: 100px;
}

.col-acceptance {
  width: 100px;
}

.col-tags {
  min-width: 160px;
}

.problem-link {
  color: #111827;
  text-decoration: none;
  font-weight: 500;
}

.problem-link:hover {
  color: #2563eb;
}

.difficulty-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
}

.difficulty-badge--easy {
  background-color: #ecfdf3;
  color: #16a34a;
}

.difficulty-badge--medium {
  background-color: #fffbeb;
  color: #d97706;
}

.difficulty-badge--hard {
  background-color: #fef2f2;
  color: #dc2626;
}

.status-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background-color: #e5e7eb;
}

.status-dot--solved {
  background-color: #22c55e;
}

.status-dot--attempted {
  background-color: #f97316;
}

.status-dot--unsolved {
  background-color: #d1d5db;
}

.tag-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background-color: #f3f4f6;
  color: #4b5563;
  font-size: 11px;
  margin-right: 4px;
}

.pagination-bar {
  padding: 10px 16px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pagination-info {
  font-size: 12px;
  color: #6b7280;
}

@media (max-width: 960px) {
  .problemset-page {
    padding: 20px 0 32px;
  }

  .problemset-filters {
    flex-direction: column;
    align-items: flex-start;
  }

  .filters-right {
    width: 100%;
    justify-content: space-between;
  }

  .search-input {
    width: 180px;
  }

  .col-tags {
    display: none;
  }
}
</style>

