<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElAside, ElContainer, ElHeader, ElMain, ElMenu, ElMenuItem } from 'element-plus'
import { Collection, DataAnalysis, PriceTag, User } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const menuItems = [
  { path: '/admin', icon: DataAnalysis, title: '数据概览' },
  { path: '/admin/users', icon: User, title: '用户管理' },
  { path: '/admin/problems', icon: Collection, title: '题库管理' },
  { path: '/admin/tags', icon: PriceTag, title: '标签管理' },
]

const activeMenu = computed(() => {
  const matched = menuItems.find((item) =>
    item.path === '/admin' ? route.path === item.path : route.path.startsWith(item.path),
  )

  return matched?.path ?? '/admin'
})

const pageTitle = computed(() => {
  const matched = menuItems.find((item) =>
    item.path === '/admin' ? route.path === item.path : route.path.startsWith(item.path),
  )

  return matched?.title ?? '管理控制台'
})

const handleMenuSelect = (path: string) => {
  router.push(path)
}
</script>

<template>
  <div class="admin-layout">
    <el-container>
      <el-aside width="220px">
        <div class="admin-logo">
          <h2>管理控制台</h2>
        </div>

        <el-menu :default-active="activeMenu" class="admin-menu" @select="handleMenuSelect">
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="admin-header">
          <h1 class="page-title">{{ pageTitle }}</h1>
        </el-header>

        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.08), transparent 30%),
    #f5f7fa;
}

.admin-layout :deep(.el-container) {
  min-height: 100vh;
}

.admin-layout :deep(.el-aside) {
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
}

.admin-logo {
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.admin-logo h2 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.admin-menu {
  border-right: none;
  padding: 10px 8px;
}

.admin-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 0;
  border-radius: 10px;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: #dbeafe;
  color: #1d4ed8;
}

.admin-header {
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e5e7eb;
  backdrop-filter: blur(14px);
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
}

.admin-main {
  padding: 24px;
  min-height: 0;
  overflow: auto;
}
</style>
