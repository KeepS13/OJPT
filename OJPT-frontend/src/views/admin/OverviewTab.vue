<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElCard, ElCol, ElMessage, ElRow, ElSkeleton, ElTag } from 'element-plus'
import {
  getJudgeEnvironmentHealth,
  getPlatformStatisticsOverview,
  getUserStatistics,
} from '@/api/admin'
import type {
  JudgeEnvironmentCheckDTO,
  JudgeEnvironmentCheckStatus,
  JudgeEnvironmentHealthDTO,
  PlatformStatisticsOverview,
  UserStatistics,
} from '@/types/admin'

const loading = ref(false)
const platformStats = ref<PlatformStatisticsOverview | null>(null)
const userStats = ref<UserStatistics | null>(null)
const judgeEnvironment = ref<JudgeEnvironmentHealthDTO | null>(null)

const checkLabels: Record<string, string> = {
  'docker-executable': 'Docker 可执行文件',
  'docker-version': 'Docker 版本检查',
  'docker-info': 'Docker 信息检查',
  'image-cpp': 'C/C++ 判题镜像',
  'image-java': 'Java 判题镜像',
  'image-python': 'Python 判题镜像',
}

const judgeChecks = computed(() => judgeEnvironment.value?.checks ?? [])

const resolveCheckLabel = (check: JudgeEnvironmentCheckDTO) =>
  checkLabels[check.name] ?? check.name

const resolveStatusLabel = (status: JudgeEnvironmentCheckStatus) => {
  if (status === 'UP') return '正常'
  if (status === 'SKIPPED') return '已跳过'
  return '异常'
}

const resolveStatusType = (status: JudgeEnvironmentCheckStatus) => {
  if (status === 'UP') return 'success'
  if (status === 'SKIPPED') return 'info'
  return 'danger'
}

const translateHealthMessage = (message?: string | null) => {
  if (!message) return '--'
  if (message === 'Judge Docker environment is healthy') return '判题 Docker 环境正常'
  if (message === 'Some judge Docker environment checks failed') return '判题 Docker 环境存在异常'
  if (message === 'Docker executable exists') return 'Docker 可执行文件存在'
  if (message === 'Docker executable path is invalid') return 'Docker 可执行文件路径无效'
  if (message === 'Docker executable found on PATH') return '已在 PATH 中找到 Docker 可执行文件'
  if (message.startsWith('Docker executable does not exist: ')) {
    return `Docker 可执行文件不存在：${message.replace('Docker executable does not exist: ', '')}`
  }
  if (message === 'Command completed successfully') return '命令执行成功'
  if (message.startsWith('Image ') && message.endsWith(' is available')) {
    return `镜像 ${message.slice(6, -13)} 可用`
  }
  if (message === 'Skipped because Docker executable is unavailable') {
    return '由于 Docker 可执行文件不可用，已跳过检查'
  }
  if (message.startsWith('Command timed out after ') && message.endsWith(' ms')) {
    const timeoutMs = message.replace('Command timed out after ', '').replace(' ms', '')
    return `命令执行超时（${timeoutMs} ms）`
  }
  if (message.startsWith('Command failed with exit code ')) {
    return message.replace('Command failed with exit code ', '命令执行失败，退出码 ')
  }
  return message
}

const loadStatistics = async () => {
  loading.value = true

  try {
    const [platformRes, userRes, healthRes] = await Promise.all([
      getPlatformStatisticsOverview(),
      getUserStatistics(),
      getJudgeEnvironmentHealth(),
    ])

    platformStats.value = platformRes.data
    userStats.value = userRes.data
    judgeEnvironment.value = healthRes.data
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err.response?.data?.message || err.message || '加载管理概览失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="overview-tab">
    <el-skeleton :loading="loading" animated :rows="8">
      <template #default>
        <section class="section">
          <div class="section-heading">
            <div>
              <h2 class="section-title">平台概览</h2>
              <p class="section-copy">展示管理后台统计接口返回的核心汇总数据。</p>
            </div>
          </div>

          <el-row v-if="platformStats" :gutter="16">
            <el-col :xs="24" :sm="24" :md="12">
              <el-card class="stat-card" shadow="never">
                <div class="stat-value">{{ platformStats.statusCount?.users || 0 }}</div>
                <div class="stat-label">注册用户数</div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="24" :md="12">
              <el-card class="stat-card stat-card--neutral" shadow="never">
                <div class="stat-value">{{ platformStats.totalCount || 0 }}</div>
                <div class="stat-label">概览总计</div>
              </el-card>
            </el-col>
          </el-row>
        </section>

        <section class="section">
          <div class="section-heading">
            <div>
              <h2 class="section-title">用户统计</h2>
              <p class="section-copy">展示当前平台账号状态分布。</p>
            </div>
          </div>

          <el-row v-if="userStats" :gutter="16">
            <el-col :xs="24" :sm="12" :md="6">
              <el-card class="stat-card stat-card--danger" shadow="never">
                <div class="stat-value">{{ userStats.statusCount?.['0'] || 0 }}</div>
                <div class="stat-label">禁用用户</div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-card class="stat-card stat-card--success" shadow="never">
                <div class="stat-value">{{ userStats.statusCount?.['1'] || 0 }}</div>
                <div class="stat-label">启用用户</div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-card class="stat-card stat-card--warning" shadow="never">
                <div class="stat-value">{{ userStats.statusCount?.['2'] || 0 }}</div>
                <div class="stat-label">待审核用户</div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-card class="stat-card stat-card--neutral" shadow="never">
                <div class="stat-value">{{ userStats.totalCount || 0 }}</div>
                <div class="stat-label">用户总数</div>
              </el-card>
            </el-col>
          </el-row>
        </section>

        <section class="section">
          <div class="section-heading">
            <div>
              <h2 class="section-title">判题环境</h2>
              <p class="section-copy">
                展示 Docker 可执行文件、`docker version` / `docker info`，以及 C/C++、Java、
                Python 判题镜像的健康状态。
              </p>
            </div>
            <el-tag
              v-if="judgeEnvironment"
              :type="resolveStatusType(judgeEnvironment.status)"
              effect="dark"
            >
              {{ resolveStatusLabel(judgeEnvironment.status) }}
            </el-tag>
          </div>

          <div v-if="judgeEnvironment" class="health-summary">
            <p class="health-message">{{ translateHealthMessage(judgeEnvironment.message) }}</p>
          </div>

          <div v-if="judgeChecks.length" class="check-grid">
            <article v-for="check in judgeChecks" :key="check.name" class="check-card">
              <div class="check-header">
                <h3 class="check-title">{{ resolveCheckLabel(check) }}</h3>
                <el-tag size="small" :type="resolveStatusType(check.status)">
                  {{ resolveStatusLabel(check.status) }}
                </el-tag>
              </div>
              <p class="check-target">{{ check.target }}</p>
              <p class="check-message">{{ translateHealthMessage(check.message) }}</p>
            </article>
          </div>
        </section>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped>
.overview-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-title {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.section-copy {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.stat-card {
  border-radius: 14px;
  border: none;
  background: #eff6ff;
}

.stat-card--success {
  background: #ecfdf5;
}

.stat-card--warning {
  background: #fffbeb;
}

.stat-card--danger {
  background: #fef2f2;
}

.stat-card--neutral {
  background: #f8fafc;
}

.stat-value {
  color: #1d4ed8;
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-card--success .stat-value {
  color: #047857;
}

.stat-card--warning .stat-value {
  color: #b45309;
}

.stat-card--danger .stat-value {
  color: #b91c1c;
}

.stat-card--neutral .stat-value {
  color: #0f172a;
}

.stat-label {
  color: #475569;
  font-size: 14px;
}

.health-summary {
  margin-bottom: 16px;
}

.health-message {
  margin: 0;
  color: #334155;
  font-size: 14px;
}

.check-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.check-card {
  padding: 16px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.check-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.check-title {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.check-target {
  margin: 0 0 8px;
  color: #1d4ed8;
  font-size: 13px;
  word-break: break-all;
}

.check-message {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
}
</style>
