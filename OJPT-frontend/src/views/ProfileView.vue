<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElUpload } from 'element-plus'
import { Form as VForm, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'
import { getCurrentUserDetail, updateUserInfo, uploadAvatar, deleteAvatar } from '@/api/user'
import { useAuth } from '@/hooks/useAuth'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { UserDetail } from '@/types/user'
import { User, EditPen, Message, Briefcase, Memo } from '@element-plus/icons-vue'

const { user: authUser } = useAuth()
const authStore = useAuthStore()

const loading = ref(false)
const userDetail = ref<UserDetail | null>(null)
// VeeValidate 表单实例，用于在异步加载完成后重置表单值
type ProfileFormContext = {
  resetForm?: (opts: { values: Record<string, unknown> }) => void
}
const formRef = ref<ProfileFormContext | null>(null)

// 表单数据
const formData = ref({
  username: '',
  email: '',
  phone: '',
  gender: 0,
  birthday: '',
  address: '',
  website: '',
  github: '',
  company: '',
  position: '',
  school: '',
  bio: '',
})

// 表单验证规则
const profileSchema = yup.object({
  email: yup.string().nullable().transform((value) => value || null).email('请输入正确的邮箱地址'),
  phone: yup.string().nullable().transform((value) => value || null).matches(/^1[3-9]\d{9}$|^$/, '请输入正确的手机号'),
  bio: yup.string().max(100, '个人介绍不能超过100个字符'),
  website: yup.string().nullable().transform((value) => value || null).url('请输入正确的网址'),
})

// 加载用户信息
const loadUserInfo = async () => {
  try {
    loading.value = true
    const res = await getCurrentUserDetail()
    const data = res.data as UserDetail

    // 将空字符串头像转换为 null，统一处理
    if (data.avatar !== undefined && (!data.avatar || !data.avatar.trim())) {
      data.avatar = null
    }

    userDetail.value = data
    // 填充表单
    formData.value = {
      username: data.username,
      email: data.email || '',
      phone: data.phone || '',
      gender: data.gender ?? 0,
      birthday: data.birthday || '',
      address: data.address || '',
      website: data.website || '',
      github: data.github || '',
      company: data.company || '',
      position: data.position || '',
      school: data.schoolId || '',
      bio: data.bio || '',
    }

    // 同步重置 VeeValidate 表单内部的值，保证输入框显示与接口数据一致
    if (formRef.value?.resetForm) {
      formRef.value.resetForm({
        values: { ...formData.value },
      })
    }
  } catch (error: unknown) {
    const err = error as { response?: { status?: number; data?: unknown }; message?: string }
    const status = err.response?.status

    // 401 会被 request.ts 拦截器自动处理（刷新 token 或清理登录态）
    // 403 表示账号被封禁或其他权限问题，直接显示错误信息
    // 其他错误也直接显示
    if (status === 401) {
      // 401 会被拦截器处理，这里不需要额外操作
      // 如果刷新失败，拦截器会清理登录态并跳转首页
      return
    }

    ElMessage.error(
      (err.response?.data as { message?: string } | undefined)?.message || err.message || '加载用户信息失败',
    )
  } finally {
    loading.value = false
  }
}

// 提交表单
interface FormValues {
  email?: string
  phone?: string
  gender?: number
  birthday?: string
  address?: string
  website?: string
  github?: string
  company?: string
  position?: string
  school?: string
  bio?: string
}

const onSubmit = async (values: FormValues) => {
  try {
    loading.value = true
    const normalizeValue = (val: string | undefined) => (val && val.trim()) || undefined

    // 直接根据表单当前值构造请求体，避免“变化对比”带来的各种边界问题
    const payload = {
      email: normalizeValue(values.email ?? formData.value.email),
      phone: normalizeValue(values.phone ?? formData.value.phone),
      gender: values.gender ?? formData.value.gender,
      birthday: normalizeValue(values.birthday ?? formData.value.birthday),
      address: normalizeValue(values.address ?? formData.value.address),
      website: normalizeValue(values.website ?? formData.value.website),
      github: normalizeValue(values.github ?? formData.value.github),
      company: normalizeValue(values.company ?? formData.value.company),
      position: normalizeValue(values.position ?? formData.value.position),
      // 学校字段转换为 schoolId（字符串），允许为空
      schoolId: (() => {
        const school = values.school ?? formData.value.school
        return school || undefined
      })(),
      bio: normalizeValue(values.bio ?? formData.value.bio),
    }

    await updateUserInfo(payload)
    ElMessage.success('个人信息更新成功')
    await loadUserInfo()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 支持的图片格式
const allowedImageTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif']

// 头像上传前处理
const beforeAvatarUpload = (file: File) => {
  const isValidType = allowedImageTypes.includes(file.type) ||
    /\.(jpg|jpeg|png|webp|gif)$/i.test(file.name)
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isValidType) {
    ElMessage.error('头像格式不支持，请上传 JPG、PNG、WebP 或 GIF 格式的图片!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('头像大小不能超过 10MB!')
    return false
  }
  return true
}

// 图片压缩函数：压缩至 320x320，质量 80%，WebP 格式
const compressImage = (file: File): Promise<File> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        // 创建 canvas
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('无法创建画布上下文'))
          return
        }

        // 计算压缩后的尺寸（保持宽高比，最大边为 320）
        let width = img.width
        let height = img.height
        const maxSize = 320

        if (width > height) {
          if (width > maxSize) {
            height = (height * maxSize) / width
            width = maxSize
          }
        } else {
          if (height > maxSize) {
            width = (width * maxSize) / height
            height = maxSize
          }
        }

        canvas.width = width
        canvas.height = height

        // 绘制图片
        ctx.drawImage(img, 0, 0, width, height)

        // 转换为 WebP 格式，质量 80%
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error('图片压缩失败'))
              return
            }
            // 创建新的 File 对象
            const compressedFile = new File([blob], file.name.replace(/\.[^.]+$/, '.webp'), {
              type: 'image/webp',
              lastModified: Date.now(),
            })
            resolve(compressedFile)
          },
          'image/webp',
          0.8 // 质量 80%
        )
      }
      img.onerror = () => {
        reject(new Error('图片加载失败'))
      }
      img.src = e.target?.result as string
    }
    reader.onerror = () => {
      reject(new Error('文件读取失败'))
    }
    reader.readAsDataURL(file)
  })
}

// 处理头像上传
const handleAvatarUpload = async (options: { file: File }) => {
  try {
    loading.value = true
    const file = options.file

    // 压缩图片
    let compressedFile: File
    try {
      compressedFile = await compressImage(file)
    } catch {
      ElMessage.error('图片压缩失败，请重试')
      return
    }

    // 检查压缩后的文件大小（如果超过 5MB，可能需要进一步处理）
    if (compressedFile.size > 5 * 1024 * 1024) {
      ElMessage.warning('图片压缩后仍然较大，正在上传...')
    }

    // 上传压缩后的图片
    const res = await uploadAvatar(compressedFile)

    // 更新 auth store 中的头像
    if (authStore.user) {
      authStore.user.avatar = res.data.avatar
    }

    // 立即更新 userDetail，避免等待 loadUserInfo 完成
    if (userDetail.value) {
      userDetail.value.avatar = res.data.avatar
    }

    ElMessage.success('头像上传成功')
    await loadUserInfo()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '头像上传失败')
  } finally {
    loading.value = false
  }
}

// 处理头像删除
const handleAvatarDelete = async () => {
  try {
    loading.value = true
    const res = await deleteAvatar()

    // 接口语义为“删除头像”，这里直接认为删除成功后头像应为空；
    // 即使后端返回了旧的 avatar，这里也强制置为 null，保证前端表现正确
    const avatarValue = (res.data.avatar && res.data.avatar.trim()) ? null : null

    // 更新 auth store 中的头像
    if (authStore.user) {
      authStore.user.avatar = avatarValue
    }

    // 立即更新 userDetail，避免等待 loadUserInfo 完成
    if (userDetail.value) {
      userDetail.value.avatar = avatarValue
    }

    ElMessage.success('头像删除成功')
    await loadUserInfo()
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '头像删除失败')
  } finally {
    loading.value = false
  }
}

// 判断是否有头像（空字符串视为无头像）
const hasAvatar = computed(() => {
  const detailAvatar = userDetail.value?.avatar
  const authAvatar = authUser.value?.avatar
  return !!(detailAvatar && detailAvatar.trim()) || !!(authAvatar && authAvatar.trim())
})

const displayName = computed(() => {
  return userDetail.value?.username || authUser.value?.username || ''
})

onMounted(() => {
  loadUserInfo()
})
</script>

<template>
  <div class="profile-view">
    <div class="profile-header">
      <h1 class="profile-title">个人资料</h1>
      <p class="profile-subtitle">管理您的个人信息和账户设置</p>
    </div>

    <div class="profile-content">
      <VForm
        ref="formRef"
        :initial-values="formData"
        :validation-schema="profileSchema"
        v-slot="{ handleSubmit: submitForm }"
      >
        <el-form
          label-position="top"
          class="profile-form"
          @submit.prevent="submitForm(onSubmit)"
        >
          <!-- 头像和用户名区域 -->
          <div class="profile-avatar-section">
            <div class="avatar-wrapper">
              <UserAvatar
                :name="displayName"
                :size="100"
                :role-type="userDetail?.roleType || authUser?.roleType"
                :avatar="userDetail?.avatar || authUser?.avatar || null"
                class="profile-avatar"
              />
              <div class="avatar-actions">
                <el-upload
                  class="avatar-uploader"
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :http-request="handleAvatarUpload"
                  accept="image/jpeg,image/jpg,image/png,image/webp,image/gif"
                >
                  <div class="avatar-action-btn avatar-edit-btn" title="更换头像">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </div>
                </el-upload>
                <div
                  v-if="hasAvatar"
                  class="avatar-action-btn avatar-delete-btn"
                  title="删除头像"
                  @click="handleAvatarDelete"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 6h18"></path>
                    <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path>
                    <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path>
                  </svg>
                </div>
              </div>
            </div>
            <div class="avatar-info">
              <h2 class="username-display">{{ displayName }}</h2>
              <p class="username-hint">可更换或删除个人头像</p>
            </div>
          </div>

          <!-- 两列布局：基本信息和联系方式 -->
          <div class="form-row">
            <!-- 左侧：基本信息 -->
            <div class="form-card form-card-left">
              <div class="card-header">
                <h3 class="card-title">
                  <el-icon class="card-icon">
                    <EditPen />
                  </el-icon>
                  基本信息
                </h3>
              </div>
              <div class="card-content">
                <el-form-item label="昵称">
                  <el-input
                    :model-value="formData.username"
                    disabled
                    placeholder="用户名不可修改"
                  />
                </el-form-item>

                <el-form-item label="性别">
                  <Field name="gender" v-slot="{ field }">
                    <el-radio-group
                      :model-value="field.value ?? formData.gender"
                      @update:model-value="(val: number) => { formData.gender = val; field.onChange(val) }"
                    >
                      <el-radio :label="1">
                        <el-icon class="gender-icon">
                          <User />
                        </el-icon>
                        男性
                      </el-radio>
                      <el-radio :label="2">
                        <el-icon class="gender-icon">
                          <User />
                        </el-icon>
                        女性
                      </el-radio>
                    </el-radio-group>
                  </Field>
                </el-form-item>

                <el-form-item label="生日">
                  <Field name="birthday" v-slot="{ field }">
                    <el-date-picker
                      :model-value="field.value ?? formData.birthday"
                      @update:model-value="(val: string | null) => { const value = val || ''; formData.birthday = value; field.onChange(value) }"
                      type="date"
                      placeholder="请选择生日"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      style="width: 100%"
                    />
                  </Field>
                </el-form-item>

                <el-form-item label="现居地">
                  <Field name="address" v-slot="{ field }">
                    <el-input
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="输入现居地"
                    />
                  </Field>
                </el-form-item>
              </div>
            </div>

            <!-- 右侧：联系方式 -->
            <div class="form-card form-card-right">
              <div class="card-header">
                <h3 class="card-title">
                  <el-icon class="card-icon">
                    <Message />
                  </el-icon>
                  联系方式
                </h3>
              </div>
              <div class="card-content">
                <el-form-item label="邮箱">
                  <Field name="email" v-slot="{ field }">
                    <el-input
                      disabled
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="邮箱不可在此修改"
                    />
                  </Field>
                  <ErrorMessage name="email" v-slot="{ message }">
                    <div class="field-error">{{ message }}</div>
                  </ErrorMessage>
                </el-form-item>

                <el-form-item label="手机号">
                  <Field name="phone" v-slot="{ field }">
                    <el-input
                      disabled
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="手机号不可在此修改"
                    />
                  </Field>
                  <ErrorMessage name="phone" v-slot="{ message }">
                    <div class="field-error">{{ message }}</div>
                  </ErrorMessage>
                </el-form-item>

                <el-form-item label="个人网站">
                  <Field name="website" v-slot="{ field }">
                    <el-input
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="https://example.com"
                    />
                  </Field>
                  <ErrorMessage name="website" v-slot="{ message }">
                    <div class="field-error">{{ message }}</div>
                  </ErrorMessage>
                </el-form-item>

                <el-form-item label="GitHub">
                  <Field name="github" v-slot="{ field }">
                    <el-input
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="GitHub 用户名"
                    />
                  </Field>
                </el-form-item>
              </div>
            </div>
          </div>

          <!-- 教育和工作信息 -->
          <div class="form-card">
            <div class="card-header">
              <h3 class="card-title">
                <el-icon class="card-icon">
                  <Briefcase />
                </el-icon>
                教育和工作
              </h3>
            </div>
            <div class="card-content">
              <div class="form-grid-three">
                <el-form-item label="就读学校">
                  <Field name="school" v-slot="{ field }">
                    <el-input
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="最高学历学校"
                    />
                  </Field>
                </el-form-item>

                <el-form-item label="所在公司">
                  <Field name="company" v-slot="{ field }">
                    <el-input
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="所在公司"
                    />
                  </Field>
                </el-form-item>

                <el-form-item label="职位">
                  <Field name="position" v-slot="{ field }">
                    <el-input
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="职位名称"
                    />
                  </Field>
                </el-form-item>
              </div>
            </div>
          </div>

          <!-- 个人介绍 -->
          <div class="form-card">
            <div class="card-header">
              <h3 class="card-title">
                <el-icon class="card-icon">
                  <Memo />
                </el-icon>
                个人介绍
              </h3>
            </div>
            <div class="card-content">
              <el-form-item>
                <Field name="bio" v-slot="{ field }">
                  <el-input
                    :model-value="field.value"
                    @update:model-value="field.onChange"
                    type="textarea"
                    :rows="4"
                    placeholder="介绍一下你自己，分享你的个性、兴趣或经验..."
                    maxlength="100"
                    show-word-limit
                  />
                </Field>
              </el-form-item>
              <ErrorMessage name="bio" v-slot="{ message }">
                <div class="field-error">{{ message }}</div>
              </ErrorMessage>
            </div>
          </div>

          <!-- 提交按钮 -->
          <div class="form-actions">
            <el-button
              type="primary"
              @click="submitForm(onSubmit)"
              :loading="loading"
              size="large"
            >
              保存修改
            </el-button>
            <el-button
              @click="loadUserInfo"
              :loading="loading"
              size="large"
            >
              重置
            </el-button>
          </div>
        </el-form>
      </VForm>
    </div>
  </div>
</template>

<style scoped>
.profile-view {
  max-width: 1000px;
  margin: 0 auto;
  background-color: #ffffff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.profile-header {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.profile-title {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.profile-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.profile-content {
  margin-top: 0;
}

/* 头像区域 */
.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 40px;
  padding: 24px;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-radius: 12px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  flex-shrink: 0;
}

.avatar-actions {
  position: absolute;
  bottom: 0;
  right: 0;
  display: flex;
  gap: 8px;
  opacity: 0;
  transform: translateY(6px);
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.avatar-wrapper:hover .avatar-actions {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.profile-avatar {
  border: 4px solid #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.avatar-uploader {
  display: inline-block;
}

.avatar-action-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #ffffff;
  border: 3px solid #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.avatar-edit-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.avatar-edit-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.avatar-delete-btn {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

.avatar-delete-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.avatar-info {
  flex: 1;
}

.username-display {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
}

.username-hint {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

/* 表单卡片 */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.form-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 24px;
  transition: all 0.2s ease;
}

.form-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.form-card-left,
.form-card-right {
  margin-bottom: 0;
}

.card-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon {
  font-size: 20px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.card-content {
  padding-top: 4px;
}

/* 表单网格布局 */
.form-grid-three {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

/* 表单样式 */
.profile-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.profile-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.profile-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
  padding: 0;
  line-height: 1.5;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 6px;
  transition: all 0.2s ease;
}

.profile-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d1d5db inset;
}

.profile-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background-color: #f9fafb;
  color: #6b7280;
}

.profile-form :deep(.el-radio-group) {
  display: flex;
  gap: 16px;
}

.profile-form :deep(.el-radio) {
  margin-right: 0;
}

.gender-icon {
  margin-right: 4px;
  font-size: 16px;
}

.profile-form :deep(.el-date-editor) {
  width: 100%;
}

.profile-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  font-family: inherit;
}

.field-error {
  color: #ef4444;
  font-size: 12px;
  margin-top: 6px;
  line-height: 1.4;
}

/* 提交按钮区域 */
.form-actions {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.form-actions .el-button {
  min-width: 120px;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-view {
    padding: 20px;
    border-radius: 8px;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .form-grid-three {
    grid-template-columns: 1fr;
  }

  .profile-avatar-section {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions .el-button {
    width: 100%;
  }
}
</style>

