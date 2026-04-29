<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElButton, ElInput, ElMessage } from 'element-plus'
import {
  createAdminTag,
  deleteAdminTag,
  getAdminTags,
  updateAdminTag,
} from '@/api/admin'
import type { TagCreateDTO, TagVO } from '@/types/admin'

interface TagFormState {
  id?: string
  name: string
  type: string
}

const loading = ref(false)
const submitting = ref(false)
const tags = ref<TagVO[]>([])
const form = ref<TagFormState>({
  name: '',
  type: '',
})

const isEditing = computed(() => Boolean(form.value.id))

const buildPayload = (): TagCreateDTO => {
  const payload: TagCreateDTO = {
    name: form.value.name.trim(),
  }

  if (form.value.type.trim()) {
    payload.type = form.value.type.trim()
  }

  return payload
}

const resetForm = () => {
  form.value = {
    name: '',
    type: '',
  }
}

const loadTags = async () => {
  loading.value = true

  try {
    const res = await getAdminTags()
    tags.value = res.data ?? []
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err.response?.data?.message || err.message || '加载标签失败')
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  const payload = buildPayload()

  if (!payload.name) {
    ElMessage.error('标签名称不能为空')
    return
  }

  submitting.value = true

  try {
    if (form.value.id) {
      await updateAdminTag(form.value.id, payload)
      ElMessage.success('标签已更新')
    } else {
      await createAdminTag(payload)
      ElMessage.success('标签已创建')
    }

    resetForm()
    await loadTags()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err.response?.data?.message || err.message || '保存标签失败')
  } finally {
    submitting.value = false
  }
}

const startEdit = (tag: TagVO) => {
  form.value = {
    id: tag.id,
    name: tag.name,
    type: tag.type ?? '',
  }
}

const handleDelete = async (tag: TagVO) => {
  if (!globalThis.confirm(`确认删除标签“${tag.name}”吗？`)) {
    return
  }

  try {
    await deleteAdminTag(tag.id)

    if (form.value.id === tag.id) {
      resetForm()
    }

    ElMessage.success('标签已删除')
    await loadTags()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err.response?.data?.message || err.message || '删除标签失败')
  }
}

onMounted(() => {
  loadTags()
})
</script>

<template>
  <div class="tag-management">
    <section class="panel form-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">标签管理</h2>
          <p class="panel-copy">创建、编辑和删除管理端题库中使用的标签。</p>
        </div>
        <div class="summary-badge">{{ tags.length }} 个标签</div>
      </div>

      <div class="form-grid">
        <label class="field">
          <span class="field-label">名称</span>
          <div data-testid="tag-name-input">
            <el-input v-model="form.name" maxlength="64" placeholder="请输入标签名称" />
          </div>
        </label>

        <label class="field">
          <span class="field-label">类型</span>
          <div data-testid="tag-type-input">
            <el-input v-model="form.type" maxlength="64" placeholder="请输入标签类型（可选）" />
          </div>
        </label>
      </div>

      <div class="form-actions">
        <el-button
          data-testid="tag-submit-button"
          type="primary"
          :loading="submitting"
          @click="submitForm"
        >
          {{ isEditing ? '保存修改' : '创建标签' }}
        </el-button>
        <el-button
          v-if="isEditing"
          data-testid="tag-cancel-edit-button"
          @click="resetForm"
        >
          取消编辑
        </el-button>
      </div>
    </section>

    <section class="panel table-panel">
      <div class="panel-header panel-header--compact">
        <div>
          <h2 class="panel-title">当前标签</h2>
          <p class="panel-copy">以下数据来自管理员标签接口。</p>
        </div>
        <el-button :loading="loading" @click="loadTags">刷新</el-button>
      </div>

      <div v-if="loading" class="table-state">正在加载标签...</div>
      <div v-else-if="!tags.length" class="table-state">暂无标签数据。</div>

      <div v-else class="table-wrapper">
        <table class="tag-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>类型</th>
              <th>ID</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tag in tags" :key="tag.id">
              <td class="tag-name">{{ tag.name }}</td>
              <td>{{ tag.type || '--' }}</td>
              <td class="tag-id">{{ tag.id }}</td>
              <td>
                <div class="action-row">
                  <button
                    :data-testid="`edit-tag-${tag.id}`"
                    class="action-button action-button--edit"
                    type="button"
                    @click="startEdit(tag)"
                  >
                    编辑
                  </button>
                  <button
                    :data-testid="`delete-tag-${tag.id}`"
                    class="action-button action-button--danger"
                    type="button"
                    @click="handleDelete(tag)"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.tag-management {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 22px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-header--compact {
  margin-bottom: 14px;
}

.panel-title {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.panel-copy {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.summary-badge {
  min-width: 92px;
  padding: 10px 14px;
  border-radius: 999px;
  background: #0f172a;
  color: #f8fafc;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.table-wrapper {
  overflow-x: auto;
}

.tag-table {
  width: 100%;
  border-collapse: collapse;
}

.tag-table th,
.tag-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  vertical-align: middle;
}

.tag-table th {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.tag-name {
  color: #0f172a;
  font-weight: 600;
}

.tag-id {
  color: #64748b;
  font-size: 13px;
}

.table-state {
  padding: 20px 4px;
  color: #64748b;
  font-size: 14px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-button {
  padding: 8px 12px;
  border: none;
  border-radius: 999px;
  background: #e2e8f0;
  color: #0f172a;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.action-button--edit {
  background: #dbeafe;
  color: #1d4ed8;
}

.action-button--danger {
  background: #fee2e2;
  color: #b91c1c;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .panel-header {
    flex-direction: column;
  }
}
</style>
