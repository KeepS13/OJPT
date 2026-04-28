<script setup lang="ts">
import { computed, type Component } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { UserFilled, Lock, Document } from '@element-plus/icons-vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useAuth } from '@/hooks/useAuth'

const route = useRoute()
const { user } = useAuth()

const displayName = computed(() => {
  return user.value?.username || user.value?.email || ''
})

type MenuIconName = 'profile' | 'security' | 'submissions'

interface MenuItem {
  path: string
  label: string
  iconName: MenuIconName
  badge?: string
}

const iconMap: Record<MenuIconName, Component> = {
  profile: UserFilled,
  security: Lock,
  submissions: Document,
}

const getIcon = (name: MenuIconName): Component => {
  return iconMap[name]
}

const menuItems: MenuItem[] = [
  { path: '/profile', label: '个人资料', iconName: 'profile' },
  { path: '/profile/submissions', label: '解题记录', iconName: 'submissions' },
  { path: '/profile/security', label: '账号安全', iconName: 'security' },
]

const isActive = (path: string) => {
  if (path === '/profile') {
    return route.path === path
  }
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<template>
  <div class="user-center-layout">
    <aside class="user-center-sidebar">
      <div class="sidebar-header">
        <UserAvatar
          :name="displayName"
          :size="64"
          :role-type="user?.roleType"
          :avatar="user?.avatar || null"
          class="sidebar-avatar"
        />
        <div class="sidebar-user-info">
          <div class="sidebar-username">
            {{ displayName }}
          </div>
        </div>
      </div>
      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="sidebar-nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <el-icon class="nav-icon">
            <component :is="getIcon(item.iconName)" />
          </el-icon>
          <span class="nav-label">{{ item.label }}</span>
          <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
        </RouterLink>
      </nav>
    </aside>
    <main class="user-center-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.user-center-layout {
  display: flex;
  min-height: calc(100vh - 60px);
  background-color: #f5f5f7;
}

.user-center-sidebar {
  width: 240px;
  background-color: #ffffff;
  border-right: 1px solid #e5e7eb;
  padding: 24px 0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 0 20px 24px;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 16px;
}

.sidebar-avatar {
  margin-bottom: 12px;
}

.sidebar-user-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-username {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 12px;
}

.sidebar-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  color: #6b7280;
  font-size: 14px;
  text-decoration: none;
  transition: all 0.2s;
  position: relative;
}

.sidebar-nav-item:hover {
  background-color: #f9fafb;
  color: #111827;
}

.sidebar-nav-item.active {
  background-color: #eff6ff;
  color: #2563eb;
  font-weight: 500;
}

.nav-icon {
  font-size: 18px;
  width: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.nav-label {
  flex: 1;
}

.nav-badge {
  font-size: 11px;
  padding: 2px 6px;
  background-color: #fce7f3;
  color: #ec4899;
  border-radius: 4px;
  font-weight: 500;
}

.user-center-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
