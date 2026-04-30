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
  },
)

const roleLabelMap: Record<string, string> = {
  USER: '用户',
  ADMIN: '管理',
}

const roleGradientMap: Record<string, string> = {
  USER: 'linear-gradient(135deg, #60a5fa, #3b82f6)',
  ADMIN: 'linear-gradient(135deg, #fb7185, #ef4444)',
}

const initials = computed(() => {
  const raw = (props.name || '').trim()
  if (raw) {
    const noSpace = raw.replace(/\s+/g, '')
    const isChineseOnly = /^[\u4e00-\u9fa5]+$/.test(noSpace)
    if (isChineseOnly && noSpace.length === 2) {
      return noSpace
    }
    if (isChineseOnly) {
      return noSpace.slice(-2)
    }
  }

  if (props.roleType) {
    return roleLabelMap[props.roleType] || '用户'
  }

  return '用户'
})

const avatarUrl = computed(() => {
  if (!props.avatar) return null
  return props.avatar
})

const avatarGradient = computed(() => {
  if (!props.roleType) {
    return 'linear-gradient(135deg, #60a5fa, #3b82f6)'
  }
  return roleGradientMap[props.roleType] || 'linear-gradient(135deg, #60a5fa, #3b82f6)'
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
