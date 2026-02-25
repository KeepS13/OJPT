<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMenu, ElMenuItem, ElContainer, ElAside, ElMain, ElHeader } from 'element-plus'
import {
  DataAnalysis,
  User,
  Key,
  Lock,
  School,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// 当前激活的菜单项
const activeMenu = ref(route.path)

// 菜单项配置
const menuItems = [
  { path: '/admin', icon: DataAnalysis, title: '数据概览' },
  { path: '/admin/users', icon: User, title: '用户管理' },
  { path: '/admin/roles', icon: Key, title: '角色管理' },
  { path: '/admin/permissions', icon: Lock, title: '权限管理' },
  { path: '/admin/schools', icon: School, title: '学校管理' },
]

const handleMenuSelect = (path: string) => {
  router.push(path)
}
</script>

<template>
  <div class="admin-layout">
    <el-container>
      <el-aside width="220px">
        <div class="admin-logo">
          <h2>管理后台</h2>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="admin-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="admin-header">
          <h1 class="page-title">{{ menuItems.find(m => m.path === route.path)?.title || '管理控制台' }}</h1>
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
  background-color: #f5f7fa;
}

.admin-layout :deep(.el-container) {
  min-height: 100vh;
}

.admin-layout :deep(.el-aside) {
  background-color: #ffffff;
  border-right: 1px solid #e5e7eb;
}

.admin-logo {
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.admin-logo h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.admin-menu {
  border-right: none;
}

.admin-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: 8px;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background-color: #eff6ff;
  color: #2563eb;
}

.admin-header {
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 60px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.admin-main {
  padding: 24px;
  background-color: #f5f7fa;
}
</style>
