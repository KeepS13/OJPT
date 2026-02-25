<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElCard, ElRow, ElCol, ElSkeleton } from 'element-plus'
import {
  getPlatformStatisticsOverview,
  getUserStatistics,
  getSchoolStatistics,
} from '@/api/admin'
import type {
  PlatformStatisticsOverview,
  UserStatistics,
  SchoolStatistics,
} from '@/types/admin'

const loading = ref(false)
const platformStats = ref<PlatformStatisticsOverview | null>(null)
const userStats = ref<UserStatistics | null>(null)
const schoolStats = ref<SchoolStatistics | null>(null)

const loadStatistics = async () => {
  try {
    loading.value = true
    const [platformRes, userRes, schoolRes] = await Promise.all([
      getPlatformStatisticsOverview(),
      getUserStatistics(),
      getSchoolStatistics(),
    ])
    platformStats.value = platformRes.data
    userStats.value = userRes.data
    schoolStats.value = schoolRes.data
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '加载统计数据失败')
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
    <el-skeleton :loading="loading" animated :rows="6">
      <template #default>
        <!-- 平台统计 -->
        <div class="section">
          <h2 class="section-title">平台统计</h2>
          <el-row :gutter="20" v-if="platformStats">
            <el-col :span="12">
              <el-card class="stat-card">
                <div class="stat-value">{{ platformStats.statusCount?.users || 0 }}</div>
                <div class="stat-label">用户总数</div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="stat-card">
                <div class="stat-value">{{ platformStats.statusCount?.schools || 0 }}</div>
                <div class="stat-label">学校总数</div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 用户统计 -->
        <div class="section">
          <h2 class="section-title">用户统计</h2>
          <el-row :gutter="20" v-if="userStats">
            <el-col :span="6">
              <el-card class="stat-card stat-card--danger">
                <div class="stat-value">{{ userStats.statusCount?.['0'] || 0 }}</div>
                <div class="stat-label">禁用用户</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="stat-card stat-card--success">
                <div class="stat-value">{{ userStats.statusCount?.['1'] || 0 }}</div>
                <div class="stat-label">启用用户</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="stat-card stat-card--warning">
                <div class="stat-value">{{ userStats.statusCount?.['2'] || 0 }}</div>
                <div class="stat-label">待审核用户</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="stat-card">
                <div class="stat-value">{{ userStats.totalCount || 0 }}</div>
                <div class="stat-label">用户总数</div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 学校统计 -->
        <div class="section">
          <h2 class="section-title">学校统计</h2>
          <el-row :gutter="20" v-if="schoolStats">
            <el-col :span="6">
              <el-card class="stat-card stat-card--danger">
                <div class="stat-value">{{ schoolStats.statusCount?.['0'] || 0 }}</div>
                <div class="stat-label">禁用学校</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="stat-card stat-card--success">
                <div class="stat-value">{{ schoolStats.statusCount?.['1'] || 0 }}</div>
                <div class="stat-label">启用学校</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="stat-card stat-card--warning">
                <div class="stat-value">{{ schoolStats.statusCount?.['2'] || 0 }}</div>
                <div class="stat-label">待认证学校</div>
              </el-card>
            </el-col>
            <el-col :span="6">
              <el-card class="stat-card">
                <div class="stat-value">{{ schoolStats.totalCount || 0 }}</div>
                <div class="stat-label">学校总数</div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped>
.overview-tab {
  padding: 0;
}

.section {
  background-color: #ffffff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 20px 0;
}

.stat-card {
  text-align: center;
  border: none;
  box-shadow: none;
  background-color: #f9fafb;
  border-radius: 8px;
}

.stat-card--success {
  background-color: #ecfdf5;
}

.stat-card--success .stat-value {
  color: #10b981;
}

.stat-card--warning {
  background-color: #fffbeb;
}

.stat-card--warning .stat-value {
  color: #f59e0b;
}

.stat-card--danger {
  background-color: #fef2f2;
}

.stat-card--danger .stat-value {
  color: #ef4444;
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
</style>
