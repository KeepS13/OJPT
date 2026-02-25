<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElInput, ElButton, ElIcon } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

interface Props {
  modelValue?: string
  placeholder?: string
  showReset?: boolean
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请输入搜索关键词',
  showReset: true,
  loading: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'search': []
  'reset': []
}>()

const keyword = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  keyword.value = val
})

watch(keyword, (val) => {
  emit('update:modelValue', val)
})

const handleSearch = () => {
  emit('search')
}

const handleReset = () => {
  keyword.value = ''
  emit('reset')
}

const handleKeyup = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    handleSearch()
  }
}
</script>

<template>
  <div class="search-bar">
    <el-input
      v-model="keyword"
      :placeholder="placeholder"
      class="search-input"
      clearable
      @keyup="handleKeyup"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
    <slot name="filters"></slot>
    <el-button type="primary" :loading="loading" @click="handleSearch">
      <el-icon><Search /></el-icon>
      搜索
    </el-button>
    <el-button v-if="showReset" @click="handleReset">
      <el-icon><Refresh /></el-icon>
      重置
    </el-button>
    <slot name="actions"></slot>
  </div>
</template>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 240px;
}
</style>
