<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    name: string
    size?: number
    roleType?: string
    avatar?: string | null
  }>(),
  {
    size: 32,
    avatar: null,
  }
)

const initials = computed(() => {
  // 优先检查用户名：如果是两个字的汉字，直接使用
  const raw = (props.name || '').trim()
  if (raw) {
    const noSpace = raw.replace(/\s+/g, '')
    const isChineseOnly = /^[\u4e00-\u9fa5]+$/.test(noSpace)
    if (isChineseOnly && noSpace.length === 2) {
      return noSpace
    }
  }

  // 其次根据 roleType 显示
  if (props.roleType) {
    const roleMap: Record<string, string> = {
      USER: '用户',
      TEACHER: '教师',
      SCHOOL: '校方',
      ADMIN: '管理',
    }
    return roleMap[props.roleType] || '用户'
  }

  // 最后根据名字生成（原有逻辑）
  if (!raw) return '用户'
  const noSpace = raw.replace(/\s+/g, '')
  const isChineseOnly = /^[\u4e00-\u9fa5]+$/.test(noSpace)
  if (isChineseOnly) {
    if (noSpace.length <= 2) return noSpace
    return noSpace.slice(-2)
  }
  return '用户'
})

// 头像地址：优先使用后端返回的 avatar，如果是相对路径则补全为完整 URL
const avatarUrl = computed(() => {
  if (!props.avatar) return null
  const path = props.avatar
  if (!path) return null
  return path.startsWith('http') ? path : `http://localhost${path}`
})

// 根据角色类型返回对应的渐变色
const avatarGradient = computed(() => {
  if (!props.roleType) {
    // 默认紫色渐变（原有颜色）
    return 'linear-gradient(135deg, #8E55EF, #E59CE7)'
  }

  const roleGradients: Record<string, string> = {
    USER: 'linear-gradient(135deg, #60a5fa, #3b82f6)', // 蓝色渐变
    TEACHER: 'linear-gradient(135deg, #34d399, #059669)', // 绿色渐变
    SCHOOL: 'linear-gradient(135deg, #fbbf24, #f59e0b)', // 黄色/橙色渐变
    ADMIN: 'linear-gradient(135deg, #fb7185, #ef4444)', // 红色渐变
  }

  return roleGradients[props.roleType] || 'linear-gradient(135deg, #8E55EF, #E59CE7)'
})

const styleVars = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  lineHeight: `${props.size}px`,
  fontSize: `${Math.round(props.size * 0.42)}px`,
  background: avatarGradient.value,
}))
</script>

<template>
  <div class="user-avatar" :style="styleVars">
    <img
      v-if="avatarUrl"
      :src="avatarUrl"
      alt="用户头像"
      class="user-avatar__img"
    />
    <span v-else class="user-avatar__text">{{ initials }}</span>
  </div>
</template>

<style scoped>
.user-avatar {
  border-radius: 50%;
  color: #ffffff;
  text-align: center;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.user-avatar__img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.user-avatar__text {
  transform: translateY(0);
  user-select: none;
}
</style>


