<script setup lang="ts">
import { computed } from 'vue'
import { ElTag } from 'element-plus'

interface Props {
  status: number | string
  type?: 'user' | 'school' | 'custom'
  customMap?: Record<number | string, { label: string; type: string }>
}

const props = withDefaults(defineProps<Props>(), {
  type: 'user'
})

// 预定义状态映射
const statusMaps = {
  user: {
    0: { label: '禁用', type: 'danger' },
    1: { label: '启用', type: 'success' },
    2: { label: '待审核', type: 'warning' },
  },
  school: {
    0: { label: '禁用', type: 'danger' },
    1: { label: '启用', type: 'success' },
    2: { label: '待认证', type: 'warning' },
  },
}

const statusConfig = computed(() => {
  if (props.type === 'custom' && props.customMap) {
    return props.customMap[props.status] || { label: '未知', type: 'info' }
  }
  
  const map = statusMaps[props.type as keyof typeof statusMaps] || statusMaps.user
  return map[props.status as keyof typeof map] || { label: '未知', type: 'info' }
})
</script>

<template>
  <el-tag :type="statusConfig.type as any" size="default">
    {{ statusConfig.label }}
  </el-tag>
</template>
