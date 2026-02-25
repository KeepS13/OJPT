<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElDialog, ElButton } from 'element-plus'

interface Props {
  modelValue: boolean
  title?: string
  content?: string
  confirmText?: string
  cancelText?: string
  confirmType?: 'primary' | 'danger' | 'warning' | 'success'
  loading?: boolean
  width?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '确认操作',
  content: '确定要执行此操作吗？',
  confirmText: '确定',
  cancelText: '取消',
  confirmType: 'primary',
  loading: false,
  width: '400px',
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
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
  >
    <div class="confirm-content">
      <slot>
        <p>{{ content }}</p>
      </slot>
    </div>
    <template #footer>
      <el-button @click="handleCancel">{{ cancelText }}</el-button>
      <el-button :type="confirmType" :loading="loading" @click="handleConfirm">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.confirm-content {
  padding: 8px 0;
}

.confirm-content p {
  margin: 0;
  color: #374151;
  line-height: 1.6;
}
</style>
