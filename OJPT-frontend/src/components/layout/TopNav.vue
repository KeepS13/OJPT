<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import LoginDialog from '@/components/auth/LoginDialog.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useAuth } from '@/hooks/useAuth'

const showLogin = ref(false)
const showMenu = ref(false)
const searchKeyword = ref('')
const router = useRouter()
const route = useRoute()
const { isAuthed, user, logout } = useAuth()

let hideTimer: ReturnType<typeof setTimeout> | null = null

const openLogin = () => {
  showLogin.value = true
}

const displayName = computed(() => {
  return user.value?.username || user.value?.email || ''
})

const roleDisplay = computed(() => {
  const code = user.value?.roleType
  if (!code) return null
  const map: Record<string, { tag: string }> = {
    USER: { tag: '用户' },
    ADMIN: { tag: '管理员' },
  }
  return map[code] ?? { tag: code }
})

const onUserEnter = () => {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
  showMenu.value = true
}

const onUserLeave = () => {
  if (hideTimer) {
    clearTimeout(hideTimer)
  }
  hideTimer = setTimeout(() => {
    showMenu.value = false
  }, 120)
}

const handleLogout = () => {
  logout()
  showMenu.value = false
  router.push('/')
}

const submitSearch = () => {
  const keyword = searchKeyword.value.trim()
  router.push({
    path: '/problemset',
    query: keyword ? { keyword } : {},
  })
}

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = typeof keyword === 'string' ? keyword : ''
  },
  { immediate: true },
)
</script>

<template>
  <header class="top-nav">
    <RouterLink to="/" class="nav-left">
      <img alt="OJPT logo" class="logo" src="@/assets/logo.svg" />
      <div class="brand">
        <span class="brand-name">OJPT</span>
        <span class="brand-desc">面向算法训练的在线刷题平台</span>
      </div>
    </RouterLink>

    <nav class="nav-center">
      <RouterLink to="/problemset">题库</RouterLink>
    </nav>

    <div class="nav-right">
      <input
        v-model="searchKeyword"
        class="nav-search"
        type="text"
        placeholder="搜索题目 / 标签 / 题号"
        data-testid="topnav-search-input"
        @keydown.enter="submitSearch"
      />
      <button
        v-if="!isAuthed"
        type="button"
        class="login-btn"
        data-testid="nav-login-button"
        @click="openLogin"
      >
        登录
      </button>
      <div
        v-else
        class="nav-user"
        @mouseenter="onUserEnter"
        @mouseleave="onUserLeave"
      >
        <UserAvatar
          :name="displayName"
          :size="32"
          :role-type="user?.roleType"
          :avatar="user?.avatar || null"
          class="nav-avatar"
        />
        <transition name="fade">
          <div v-if="showMenu" class="user-menu">
            <div class="user-menu__header">
              <div class="user-menu__name">{{ displayName || '用户' }}</div>
              <div class="user-menu__role" v-if="roleDisplay">
                <span class="role-badge" :class="`role-badge--${user?.roleType?.toLowerCase()}`">
                  {{ roleDisplay.tag }}
                </span>
              </div>
            </div>
            <div class="user-menu__body">
              <RouterLink to="/profile" class="user-menu__item">个人中心</RouterLink>
              <RouterLink
                v-if="user?.roles?.includes('ADMIN')"
                to="/admin"
                class="user-menu__item"
              >
                管理员控制台
              </RouterLink>
              <RouterLink to="/profile/security" class="user-menu__item">账号安全</RouterLink>
            </div>
            <div class="user-menu__footer">
              <button type="button" class="user-menu__logout" @click="handleLogout">
                退出登录
              </button>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </header>

  <LoginDialog v-model="showLogin" />
</template>

<style scoped>
.top-nav {
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
  color: inherit;
}

.logo {
  width: 32px;
  height: 32px;
}

.brand {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-name {
  font-weight: 600;
  font-size: 16px;
  color: #111827;
}

.brand-desc {
  font-size: 12px;
  color: #6b7280;
}

.nav-center {
  display: flex;
  align-items: center;
  gap: 24px;
  font-size: 14px;
}

.nav-center a {
  color: #4b5563;
  padding: 4px 6px;
  border-radius: 4px;
}

.nav-center a.router-link-exact-active {
  color: #111827;
  font-weight: 600;
}

.nav-center a:hover {
  background-color: #f3f4f6;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-search {
  width: 260px;
  height: 34px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  font-size: 13px;
  outline: none;
}

.nav-search:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.3);
}

.login-btn {
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid #2563eb;
  color: #2563eb;
  font-size: 13px;
}

.login-btn:hover {
  background-color: #eff6ff;
}

.nav-avatar {
  cursor: pointer;
}

.nav-user {
  position: relative;
}

.user-menu {
  position: absolute;
  right: 0;
  top: 44px;
  width: 220px;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  padding: 10px 0 8px;
  z-index: 20;
}

.user-menu__header {
  padding: 0 16px 6px;
  border-bottom: 1px solid #f3f4f6;
}

.user-menu__name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.user-menu__role {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #ff6bb3, #ff3c7d);
  user-select: none;
}

.role-badge--user {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}

.role-badge--admin {
  background: linear-gradient(135deg, #fb7185, #ef4444);
}

.user-menu__body {
  padding: 4px 0;
}

.user-menu__item {
  display: block;
  padding: 6px 16px;
  font-size: 13px;
  color: #374151;
}

.user-menu__item:hover {
  background-color: #f9fafb;
}

.user-menu__footer {
  padding: 4px 16px 0;
  border-top: 1px solid #f3f4f6;
  margin-top: 4px;
}

.user-menu__logout {
  width: 100%;
  border: 0;
  background: transparent;
  color: #ef4444;
  font-size: 13px;
  text-align: left;
  padding: 6px 0;
  cursor: pointer;
}

.user-menu__logout:hover {
  color: #b91c1c;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 768px) {
  .top-nav {
    padding: 0 16px;
    gap: 8px;
  }

  .nav-center {
    display: none;
  }

  .nav-search {
    width: 160px;
  }
}
</style>
