<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import LoginDialog from '@/components/auth/LoginDialog.vue'
import { useAuth } from '@/hooks/useAuth'

const { isAuthed } = useAuth()
const showLogin = ref(false)

const primaryAction = computed(() => (isAuthed.value ? '继续练习' : '进入题库'))
const secondaryAction = computed(() => (isAuthed.value ? '查看提交记录' : '登录同步草稿'))

const openLogin = () => {
  showLogin.value = true
}

const overviewItems = [
  { label: '真实判题', value: '逐用例' },
  { label: '草稿自动同步', value: '在线保存' },
  { label: '提交排名', value: 'AC 后展示' },
  { label: '耗时分布', value: '区间统计' },
]

const quickProblems = [
  { no: 1, title: 'P0001', meta: '入门热身' },
  { no: 2, title: 'P0002', meta: '字符串练习' },
  { no: 3, title: 'P0003', meta: '基础算法' },
]

const features = [
  {
    title: '真实提交判题',
    text: '逐用例运行，失败即停，保留编译、运行和答案错误详情。',
  },
  {
    title: '代码草稿同步',
    text: '作答页自动保存，Ctrl/Cmd+S 可手动保存当前代码。',
  },
  {
    title: '排名和耗时分布',
    text: 'AC 后展示当前排名，并给出耗时区间分布参考。',
  },
]
</script>

<template>
  <section class="home-workbench">
    <div class="workbench-hero">
      <div class="hero-main">
        <div class="hero-label">OJPT 训练</div>
        <h1>OJPT 训练工作台</h1>
        <p class="hero-subtitle">
          从题库进入作答，保存草稿，提交后查看排名和耗时分布。
        </p>

        <div class="hero-actions">
          <RouterLink to="/problemset" class="action-button action-button--primary">
            {{ primaryAction }}
          </RouterLink>
          <RouterLink
            v-if="isAuthed"
            to="/profile/submissions"
            class="action-button action-button--secondary"
          >
            {{ secondaryAction }}
          </RouterLink>
          <button
            v-else
            type="button"
            class="action-button action-button--secondary"
            @click="openLogin"
          >
            {{ secondaryAction }}
          </button>
          <RouterLink v-if="isAuthed" to="/profile" class="action-link">
            个人中心
          </RouterLink>
        </div>
      </div>

      <aside class="overview-card" aria-label="训练概览">
        <div class="overview-card__header">
          <div>
            <h2>训练概览</h2>
            <p>提交、草稿和结果反馈都在作答流里完成。</p>
          </div>
          <RouterLink to="/problemset" class="overview-card__link">题库</RouterLink>
        </div>

        <div class="overview-grid">
          <div v-for="item in overviewItems" :key="item.label" class="overview-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <div class="quick-section">
          <div class="quick-section__title">快捷入口</div>
          <div class="quick-list">
            <RouterLink
              v-for="problem in quickProblems"
              :key="problem.no"
              :to="`/problems/${problem.no}`"
              class="quick-problem"
            >
              <span class="quick-problem__no">{{ problem.title }}</span>
              <span class="quick-problem__meta">{{ problem.meta }}</span>
            </RouterLink>
          </div>
        </div>
      </aside>
    </div>

    <div class="feature-grid">
      <article v-for="feature in features" :key="feature.title" class="feature-card">
        <h2>{{ feature.title }}</h2>
        <p>{{ feature.text }}</p>
      </article>
    </div>
  </section>

  <LoginDialog v-model="showLogin" />
</template>

<style scoped>
.home-workbench {
  min-height: 100%;
  padding: 24px 16px 32px;
  background: #f6f7f9;
}

.workbench-hero {
  max-width: 1120px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 0.95fr);
  gap: 16px;
  align-items: stretch;
}

.hero-main,
.overview-card,
.feature-card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
}

.hero-main {
  min-height: 310px;
  padding: 34px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-label {
  width: fit-content;
  padding: 4px 9px;
  border-radius: 6px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 12px;
  font-weight: 700;
}

.hero-main h1 {
  margin: 16px 0 10px;
  color: #111827;
  font-size: 40px;
  line-height: 1.12;
  font-weight: 750;
}

.hero-subtitle {
  max-width: 520px;
  margin: 0;
  color: #4b5563;
  font-size: 15px;
  line-height: 1.7;
}

.hero-actions {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.action-button,
.action-link {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.action-button {
  min-width: 116px;
  padding: 0 16px;
  border: 1px solid transparent;
  cursor: pointer;
}

.action-button--primary {
  color: #ffffff;
  background: #2563eb;
}

.action-button--primary:hover {
  background: #1d4ed8;
}

.action-button--secondary {
  color: #1f2937;
  background: #ffffff;
  border-color: #d1d5db;
}

.action-button--secondary:hover {
  background: #f9fafb;
}

.action-link {
  padding: 0 6px;
  color: #2563eb;
}

.overview-card {
  padding: 20px;
}

.overview-card__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.overview-card h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.overview-card p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.overview-card__link {
  flex: 0 0 auto;
  padding: 6px 10px;
  border-radius: 7px;
  color: #2563eb;
  background: #eff6ff;
  font-size: 13px;
  font-weight: 700;
}

.overview-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.overview-item {
  padding: 12px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  background: #fafafa;
}

.overview-item span,
.quick-section__title,
.quick-problem__meta {
  color: #6b7280;
  font-size: 12px;
}

.overview-item strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 14px;
}

.quick-section {
  margin-top: 18px;
}

.quick-list {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.quick-problem {
  min-width: 0;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.quick-problem:hover {
  border-color: #93c5fd;
  background: #f8fbff;
}

.quick-problem__no {
  display: block;
  color: #111827;
  font-size: 13px;
  font-weight: 750;
}

.quick-problem__meta {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feature-grid {
  max-width: 1120px;
  margin: 14px auto 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.feature-card {
  padding: 16px;
}

.feature-card h2 {
  margin: 0 0 8px;
  color: #111827;
  font-size: 16px;
}

.feature-card p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.65;
}

@media (max-width: 960px) {
  .workbench-hero,
  .feature-grid {
    grid-template-columns: 1fr;
  }

  .hero-main {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .home-workbench {
    padding: 14px 8px 24px;
  }

  .hero-main,
  .overview-card,
  .feature-card {
    border-radius: 8px;
  }

  .hero-main {
    padding: 22px 18px;
  }

  .hero-main h1 {
    font-size: 30px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .action-button,
  .action-link {
    width: 100%;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .quick-list {
    display: flex;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .quick-problem {
    min-width: 120px;
  }
}
</style>
