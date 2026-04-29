<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getCurrentUserTrainingDashboard,
  type TrainingDashboardRecentSubmission,
  type UserTrainingDashboard,
} from '@/api/user'

const loading = ref(false)
const dashboard = ref<UserTrainingDashboard | null>(null)

const difficultyLabelMap: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难',
  UNKNOWN: '未标注',
}

const metricCards = computed(() => [
  {
    key: 'total',
    label: '总提交数',
    value: String(dashboard.value?.totalSubmissions ?? 0),
    tone: 'neutral',
  },
  {
    key: 'accepted',
    label: 'AC 数',
    value: String(dashboard.value?.acceptedSubmissions ?? 0),
    tone: 'success',
  },
  {
    key: 'solved',
    label: '已解决题数',
    value: String(dashboard.value?.solvedProblemCount ?? 0),
    tone: 'accent',
  },
  {
    key: 'rate',
    label: '通过率',
    value: formatPercent(dashboard.value?.acceptanceRate ?? 0),
    tone: 'warning',
  },
])

const statusItems = computed(() => {
  return Object.entries(dashboard.value?.statusDistribution ?? {}).map(([key, count]) => ({
    key,
    label: key,
    count,
  }))
})

const difficultyItems = computed(() => {
  return Object.entries(dashboard.value?.difficultyDistribution ?? {}).map(([key, count]) => ({
    key,
    label: difficultyLabelMap[key] ?? key,
    count,
  }))
})

const recentSubmissions = computed<TrainingDashboardRecentSubmission[]>(() => {
  return dashboard.value?.recentSubmissions ?? []
})

const maxStatusCount = computed(() => {
  return Math.max(1, ...statusItems.value.map((item) => item.count))
})

const maxDifficultyCount = computed(() => {
  return Math.max(1, ...difficultyItems.value.map((item) => item.count))
})

const loadDashboard = async () => {
  try {
    loading.value = true
    const res = await getCurrentUserTrainingDashboard()
    dashboard.value = res.data
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载训练看板失败')
  } finally {
    loading.value = false
  }
}

const formatPercent = (value: number) => {
  return `${Number(value.toFixed(1)).toString()}%`
}

const formatDateTime = (value: string) => {
  return value ? value.replace('T', ' ') : '--'
}

const formatProblemNo = (problemNo?: number | null) => {
  if (problemNo == null) {
    return 'P----'
  }
  return `P${String(problemNo).padStart(4, '0')}`
}

const getBarWidth = (count: number, max: number) => {
  return `${Math.max(12, Math.round((count / max) * 100))}%`
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <div class="training-dashboard-view">
    <section class="dashboard-hero">
      <div>
        <p class="hero-kicker">个人训练数据面板</p>
        <h1 class="hero-title">训练看板</h1>
        <p class="hero-subtitle">
          聚合查看你的提交活跃度、通过情况、最近提交与解题难度分布。
        </p>
      </div>
    </section>

    <section class="metrics-grid">
      <el-card
        v-for="metric in metricCards"
        :key="metric.key"
        shadow="never"
        class="metric-card"
        :class="`metric-card--${metric.tone}`"
      >
        <div class="metric-label">{{ metric.label }}</div>
        <div class="metric-value">{{ metric.value }}</div>
      </el-card>
    </section>

    <section class="dashboard-grid">
      <el-card shadow="never" class="panel-card" v-loading="loading">
        <div class="panel-header">
          <h2>状态分布</h2>
          <span>{{ statusItems.length }} 类结果</span>
        </div>
        <div v-if="statusItems.length" class="distribution-list">
          <div v-for="item in statusItems" :key="item.key" class="distribution-item">
            <div class="distribution-row">
              <span class="distribution-label">{{ item.label }}</span>
              <span class="distribution-value">{{ item.count }}</span>
            </div>
            <div class="distribution-track">
              <div
                class="distribution-bar distribution-bar--status"
                :style="{ width: getBarWidth(item.count, maxStatusCount) }"
              />
            </div>
          </div>
        </div>
        <div v-else class="empty-state">暂无提交状态数据</div>
      </el-card>

      <el-card shadow="never" class="panel-card" v-loading="loading">
        <div class="panel-header">
          <h2>难度分布</h2>
          <span>{{ difficultyItems.length }} 个难度层级</span>
        </div>
        <div v-if="difficultyItems.length" class="distribution-list">
          <div v-for="item in difficultyItems" :key="item.key" class="distribution-item">
            <div class="distribution-row">
              <span class="distribution-label">{{ item.label }}</span>
              <span class="distribution-value">{{ item.count }}</span>
            </div>
            <div class="distribution-track">
              <div
                class="distribution-bar distribution-bar--difficulty"
                :style="{ width: getBarWidth(item.count, maxDifficultyCount) }"
              />
            </div>
          </div>
        </div>
        <div v-else class="empty-state">暂无解题难度数据</div>
      </el-card>
    </section>

    <el-card shadow="never" class="recent-card" v-loading="loading">
      <div class="panel-header">
        <h2>最近提交</h2>
        <span>{{ recentSubmissions.length }} 条</span>
      </div>

      <div v-if="recentSubmissions.length" class="recent-list">
        <article
          v-for="submission in recentSubmissions"
          :key="submission.submissionId"
          class="recent-item"
        >
          <div class="recent-main">
            <div class="recent-problem">
              <span class="problem-pill">{{ formatProblemNo(submission.problemNo) }}</span>
              <strong>{{ submission.problemTitle || '未知题目' }}</strong>
            </div>
            <div class="recent-meta">
              <span class="status-chip">{{ submission.status }}</span>
              <span>{{ submission.language }}</span>
              <span>{{ formatDateTime(submission.createdAt) }}</span>
            </div>
          </div>
          <div class="recent-stats">
            <span>{{ submission.timeMs ?? '--' }} ms</span>
            <span>{{ submission.memoryKb ?? '--' }} KB</span>
          </div>
        </article>
      </div>
      <div v-else class="empty-state">最近还没有提交记录</div>
    </el-card>
  </div>
</template>

<style scoped>
.training-dashboard-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-hero {
  border-radius: 24px;
  padding: 28px 30px;
  color: #0f172a;
  background:
    radial-gradient(circle at top right, rgba(249, 115, 22, 0.16), transparent 28%),
    radial-gradient(circle at left center, rgba(14, 165, 233, 0.16), transparent 30%),
    linear-gradient(135deg, #fff9f1 0%, #f7fbff 48%, #fef3c7 100%);
  border: 1px solid #f0dcc2;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-kicker {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #9a3412;
}

.hero-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.1;
}

.hero-subtitle {
  margin: 12px 0 0;
  max-width: 720px;
  color: #475569;
  line-height: 1.7;
  font-size: 14px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  border-radius: 18px;
  border: 1px solid #e5e7eb;
}

.metric-card--neutral {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.metric-card--success {
  background: linear-gradient(180deg, #f0fdf4 0%, #dcfce7 100%);
}

.metric-card--accent {
  background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
}

.metric-card--warning {
  background: linear-gradient(180deg, #fff7ed 0%, #ffedd5 100%);
}

.metric-label {
  color: #64748b;
  font-size: 13px;
}

.metric-value {
  margin-top: 10px;
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.04em;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.panel-card,
.recent-card {
  border-radius: 20px;
  border: 1px solid #e5e7eb;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-header h2 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.panel-header span {
  font-size: 13px;
  color: #64748b;
}

.distribution-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.distribution-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.distribution-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.distribution-label {
  color: #1e293b;
  font-weight: 500;
}

.distribution-value {
  color: #475569;
  font-variant-numeric: tabular-nums;
}

.distribution-track {
  height: 10px;
  border-radius: 999px;
  background: #f1f5f9;
  overflow: hidden;
}

.distribution-bar {
  height: 100%;
  border-radius: inherit;
}

.distribution-bar--status {
  background: linear-gradient(90deg, #2563eb 0%, #38bdf8 100%);
}

.distribution-bar--difficulty {
  background: linear-gradient(90deg, #ea580c 0%, #f59e0b 100%);
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e8edf3;
}

.recent-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recent-problem {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #0f172a;
}

.problem-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #dbe2ea;
  font-size: 12px;
  color: #475569;
  font-family: 'Courier New', Courier, monospace;
}

.recent-meta,
.recent-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.status-chip {
  padding: 2px 8px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 600;
}

.empty-state {
  color: #94a3b8;
  font-size: 14px;
}

@media (max-width: 960px) {
  .metrics-grid,
  .dashboard-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-hero {
    padding: 22px;
  }

  .metrics-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .recent-item {
    flex-direction: column;
  }
}
</style>
