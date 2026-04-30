import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUser } from '@/api/auth'
import { ensureAuthReady } from '@/hooks/useAuth'
import type { RoleType } from '@/utils/role'

const HomeView = () => import('@/views/HomeView.vue')
const ProblemSetView = () => import('@/views/ProblemSetView.vue')
const ProblemSolveView = () => import('@/views/ProblemSolveView.vue')
const NotFoundView = () => import('@/views/NotFoundView.vue')
const UserCenterLayout = () => import('@/components/layout/UserCenterLayout.vue')
const ProfileView = () => import('@/views/ProfileView.vue')
const SecurityView = () => import('@/views/SecurityView.vue')
const SubmissionRecordsView = () => import('@/views/SubmissionRecordsView.vue')
const TrainingDashboardView = () => import('@/views/TrainingDashboardView.vue')
const AdminLayout = () => import('@/views/admin/AdminLayout.vue')
const OverviewTab = () => import('@/views/admin/OverviewTab.vue')
const UserManagement = () => import('@/views/admin/UserManagement.vue')
const ProblemManagement = () => import('@/views/admin/ProblemManagement.vue')
const ProblemEdit = () => import('@/views/admin/ProblemEdit.vue')
const TagManagement = () => import('@/views/admin/TagManagement.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/problemset',
      name: 'problemset',
      component: ProblemSetView,
    },
    {
      path: '/problems/:problemNo',
      name: 'problem-solve',
      component: ProblemSolveView,
      meta: { hideTopNav: true },
    },
    {
      path: '/profile',
      component: UserCenterLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'profile',
          component: ProfileView,
        },
        {
          path: 'training',
          name: 'training-dashboard',
          component: TrainingDashboardView,
        },
        {
          path: 'security',
          name: 'security',
          component: SecurityView,
        },
        {
          path: 'submissions',
          name: 'submission-records',
          component: SubmissionRecordsView,
        },
      ],
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, requiredRole: 'ADMIN' },
      children: [
        {
          path: '',
          name: 'admin-overview',
          component: OverviewTab,
        },
        {
          path: 'users',
          name: 'admin-users',
          component: UserManagement,
        },
        {
          path: 'problems',
          name: 'admin-problems',
          component: ProblemManagement,
        },
        {
          path: 'problems/:problemId',
          name: 'admin-problem-edit',
          component: ProblemEdit,
        },
        {
          path: 'tags',
          name: 'admin-tags',
          component: TagManagement,
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
    },
  ],
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  await ensureAuthReady()

  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth
  const requiredRole = to.meta.requiredRole as RoleType | undefined

  // 如果有 token 但未加载用户信息，先尝试加载一次
  if (requiresAuth && authStore.accessToken && !authStore.user) {
    try {
      const res = await getCurrentUser()
      authStore.setUserProfile({
        userId: typeof res.data.userId === 'number' ? String(res.data.userId) : res.data.userId,
        username: res.data.username,
        email: res.data.email,
        avatar: res.data.avatar && res.data.avatar.trim() ? res.data.avatar : null,
        roleType: res.data.roleType,
        roles: res.data.roles,
      })
    } catch {
      authStore.clear()
    }
  }

  // 检查是否需要登录
  if (requiresAuth && !authStore.accessToken) {
    ElMessage.warning('请先登录')
    next({ path: '/', replace: true })
    return
  }

  // 检查角色权限
  if (requiresAuth && requiredRole) {
    const userRoles = authStore.user?.roles || []
    if (!userRoles.includes(requiredRole)) {
      ElMessage.error('您没有权限访问此页面')
      next({ path: '/', replace: true })
      return
    }
  }

  next()
})

export default router
