<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElDialog, ElButton } from 'element-plus'

interface Props {
  modelValue: boolean
  title?: string
  confirmText?: string
  cancelText?: string
  loading?: boolean
  width?: string
  showFooter?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '表单',
  confirmText: '确定',
  cancelText: '取消',
  loading: false,
  width: '500px',
  showFooter: true,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'confirm': []
  'cancel': []
}>()

const visible = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  visible.value = false
  emit('cancel')
}

// 暴露关闭方法
const close = () => {
  visible.value = false
}

defineExpose({ close })
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <slot></slot>
    <template #footer v-if="showFooter">
      <slot name="footer">
        <el-button @click="handleCancel">{{ cancelText }}</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">
          {{ confirmText }}
        </el-button>
      </slot>
    </template>
  </el-dialog>
</template>
