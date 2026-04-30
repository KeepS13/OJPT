<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElDialog } from 'element-plus'
import { getCurrentUserDetail, updateUsername, updateEmail, updatePhone, updatePassword, deleteAccount } from '@/api/user'
import { useAuth } from '@/hooks/useAuth'
import { useAuthStore } from '@/stores/auth'
import { stopTokenRefreshTimer } from '@/api/request'
import { clearTokens } from '@/utils/storage'
import { useRouter } from 'vue-router'
import type { UserDetail } from '@/types/user'

const { user: authUser } = useAuth()
const authStore = useAuthStore()
const router = useRouter()
const loading = ref(false)
const userDetail = ref<UserDetail | null>(null)
const showFirstConfirm = ref(false)
const showSecondConfirm = ref(false)

// 弹窗状态
const showUsernameDialog = ref(false)
const showEmailDialog = ref(false)
const showPhoneDialog = ref(false)
const showPasswordDialog = ref(false)

// 表单数据
const usernameForm = ref({ username: '' })
const emailForm = ref({ email: '' })
const phoneForm = ref({ phone: '' })
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 表单引用（直接从 element-plus 动态获取类型，避免显式导入组件类型）
type ElFormInstance = InstanceType<typeof import('element-plus')['ElForm']>
const usernameFormRef = ref<ElFormInstance | null>(null)
const emailFormRef = ref<ElFormInstance | null>(null)
const phoneFormRef = ref<ElFormInstance | null>(null)
const passwordFormRef = ref<ElFormInstance | null>(null)

// 表单验证规则
const usernameRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为2-20个字符', trigger: 'blur' },
  ],
}

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
}

const phoneRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8个字符', trigger: 'blur' },
    {
      validator: (rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value && !/(?=.*[a-zA-Z])(?=.*\d)/.test(value)) {
          callback(new Error('密码必须包含字母和数字'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    loading.value = true
    const res = await getCurrentUserDetail()
    userDetail.value = res.data as UserDetail
  } catch (error: unknown) {
    const err = error as { response?: { status?: number; data?: unknown }; message?: string }
    const status = err.response?.status

    if (status === 401) {
      return
    }

    ElMessage.error(
      (err.response?.data as { message?: string } | undefined)?.message || err.message || '加载用户信息失败',
    )
  } finally {
    loading.value = false
  }
}

// 格式化手机号显示（脱敏）
const formatPhone = (phone: string | undefined) => {
  if (!phone) return '未添加手机号'
  if (phone.length === 11) {
    return `+86 ${phone.slice(0, 3)}****${phone.slice(-4)}`
  }
  return phone
}

// 处理修改用户名
const handleEditUsername = () => {
  usernameForm.value.username = userDetail.value?.username || authUser.value?.username || ''
  showUsernameDialog.value = true
}

// 提交修改用户名
const submitUsername = async () => {
  if (!usernameFormRef.value) return
  await usernameFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        await updateUsername({ username: usernameForm.value.username })
        ElMessage.success('用户名修改成功')
        showUsernameDialog.value = false
        // 更新本地用户信息
        if (authStore.user) {
          authStore.user.username = usernameForm.value.username
        }
        await loadUserInfo()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '修改用户名失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 处理修改邮箱
const handleEditEmail = () => {
  emailForm.value.email = userDetail.value?.email || authUser.value?.email || ''
  showEmailDialog.value = true
}

// 提交修改邮箱
const submitEmail = async () => {
  if (!emailFormRef.value) return
  await emailFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        await updateEmail({ email: emailForm.value.email })
        ElMessage.success('邮箱修改成功')
        showEmailDialog.value = false
        // 更新本地用户信息
        if (authStore.user) {
          authStore.user.email = emailForm.value.email
        }
        await loadUserInfo()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '修改邮箱失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 处理修改手机号
const handleEditPhone = () => {
  phoneForm.value.phone = userDetail.value?.phone || ''
  showPhoneDialog.value = true
}

// 提交修改手机号
const submitPhone = async () => {
  if (!phoneFormRef.value) return
  await phoneFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        await updatePhone({ phone: phoneForm.value.phone })
        ElMessage.success('手机号修改成功')
        showPhoneDialog.value = false
        await loadUserInfo()
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '修改手机号失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 处理重置密码
const handleResetPassword = () => {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  showPasswordDialog.value = true
}

// 提交修改密码
const submitPassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        loading.value = true
        await updatePassword({
          oldPassword: passwordForm.value.oldPassword,
          newPassword: passwordForm.value.newPassword,
        })
        ElMessage.success('密码修改成功')
        showPasswordDialog.value = false
        passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        ElMessage.error(err?.response?.data?.message || err?.message || '修改密码失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 处理注销账号 - 打开第一次确认
const handleDeleteAccount = () => {
  showFirstConfirm.value = true
}

// 处理第一次确认 - 继续
const handleFirstConfirm = () => {
  showFirstConfirm.value = false
  // 显示第二次确认弹窗（按钮顺序相反）
  showSecondConfirm.value = true
}

// 处理第一次确认 - 取消
const handleFirstCancel = () => {
  showFirstConfirm.value = false
}

// 处理第二次确认 - 确定注销
const handleConfirmDelete = async () => {
  try {
    loading.value = true
    await deleteAccount()
    ElMessage.success('账号注销成功')
    showSecondConfirm.value = false
    // 清理登录状态并跳转首页
    stopTokenRefreshTimer()
    authStore.clear()
    clearTokens()
    router.push('/')
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } }; message?: string }
    ElMessage.error(err?.response?.data?.message || err?.message || '注销账号失败')
  } finally {
    loading.value = false
  }
}

// 处理第二次确认 - 取消
const handleCancelDelete = () => {
  showSecondConfirm.value = false
}

onMounted(() => {
  loadUserInfo()
})
</script>

<template>
  <div class="security-view">
    <div class="security-header">
      <h1 class="security-title">账号安全</h1>
    </div>

    <div class="security-content">
      <!-- 账号信息 -->
      <div class="section">
        <h2 class="section-title">账号信息</h2>
        <div class="info-list">
          <!-- 用户名 -->
          <div class="info-row">
            <div class="info-left">
              <div class="info-label">用户名</div>
              <div class="info-value">
                {{ userDetail?.username || authUser?.username || '未设置' }}
              </div>
            </div>
            <div class="info-right">
              <button class="edit-btn" @click="handleEditUsername">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                <span>编辑</span>
              </button>
            </div>
          </div>

          <!-- 电子邮箱 -->
          <div class="info-row">
            <div class="info-left">
              <div class="info-label">电子邮箱</div>
              <div class="info-value">
                {{ userDetail?.email || authUser?.email || '未添加邮箱' }}
              </div>
            </div>
            <div class="info-right">
              <button class="edit-btn" @click="handleEditEmail">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                <span>编辑</span>
              </button>
            </div>
          </div>

          <!-- 手机号 -->
          <div class="info-row">
            <div class="info-left">
              <div class="info-label">手机号</div>
              <div class="info-value">
                {{ formatPhone(userDetail?.phone) }}
              </div>
            </div>
            <div class="info-right">
              <button class="edit-btn" @click="handleEditPhone">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                <span>编辑</span>
              </button>
            </div>
          </div>

          <!-- 密码 -->
          <div class="info-row">
            <div class="info-left">
              <div class="info-label">密码</div>
              <div class="info-value">********</div>
            </div>
            <div class="info-right">
              <button class="edit-btn" @click="handleResetPassword">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                <span>重置</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 注销账号 -->
      <div class="section">
        <div class="delete-section">
          <button class="delete-btn" @click="handleDeleteAccount">注销账号</button>
        </div>
      </div>
    </div>

    <!-- 修改用户名弹窗 -->
    <ElDialog v-model="showUsernameDialog" title="修改用户名" width="420px" :close-on-click-modal="false">
      <el-form ref="usernameFormRef" :model="usernameForm" :rules="usernameRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="usernameForm.username" placeholder="请输入新用户名" maxlength="20" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showUsernameDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitUsername">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 修改邮箱弹窗 -->
    <ElDialog v-model="showEmailDialog" title="修改邮箱" width="420px" :close-on-click-modal="false">
      <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="80px">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="emailForm.email" placeholder="请输入新邮箱" type="email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEmailDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitEmail">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 修改手机号弹窗 -->
    <ElDialog v-model="showPhoneDialog" title="修改手机号" width="420px" :close-on-click-modal="false">
      <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-width="80px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="phoneForm.phone" placeholder="请输入新手机号" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showPhoneDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitPhone">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 修改密码弹窗 -->
    <ElDialog v-model="showPasswordDialog" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少8位，包含字母和数字）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showPasswordDialog = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitPassword">确定</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 第一次确认弹窗 -->
    <ElDialog
      v-model="showFirstConfirm"
      title="注销账号"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      class="delete-confirm-dialog"
    >
      <div class="confirm-content">
        <div class="confirm-icon warning-icon">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#e6a23c"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
            <line x1="12" y1="9" x2="12" y2="13"></line>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
        </div>
        <div class="confirm-message">注销账号后，您的所有数据将被永久删除且无法恢复。确定要继续吗？</div>
      </div>
      <template #footer>
        <div class="dialog-footer dialog-footer-normal">
          <el-button @click="handleFirstCancel">取消</el-button>
          <el-button type="primary" class="el-button--danger" @click="handleFirstConfirm">确定注销</el-button>
        </div>
      </template>
    </ElDialog>

    <!-- 第二次确认弹窗（按钮顺序相反） -->
    <ElDialog
      v-model="showSecondConfirm"
      title="最后确认"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      class="delete-confirm-dialog"
    >
      <div class="confirm-content">
        <div class="confirm-icon error-icon">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#f56c6c"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
        </div>
        <div class="confirm-message">请再次确认，此操作不可撤销！</div>
      </div>
      <template #footer>
        <div class="dialog-footer dialog-footer-reverse">
          <el-button type="primary" class="el-button--danger" :loading="loading" @click="handleConfirmDelete">
            确定注销
          </el-button>
          <el-button @click="handleCancelDelete">取消</el-button>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.security-view {
  max-width: 800px;
  margin: 0 auto;
  background: linear-gradient(180deg, #ffffff 0%, #fcfcfd 100%);
  border: 1px solid #e7ebf1;
  border-radius: 18px;
  padding: 34px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.security-header {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e8edf3;
}

.security-title {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.03em;
}

.security-content {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.section {
  width: 100%;
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 16px 0;
}

.info-list {
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border: 1px solid #e6ebf2;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.035);
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid #eef2f7;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row:hover {
  background-color: #f8fbff;
}

.info-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 400;
}

.info-value {
  font-size: 15px;
  color: #0f172a;
  font-weight: 500;
  line-height: 1.5;
}

.info-right {
  display: flex;
  align-items: center;
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(37, 99, 235, 0.06);
  border: 1px solid rgba(37, 99, 235, 0.1);
  color: #2563eb;
  font-size: 14px;
  cursor: pointer;
  border-radius: 999px;
  transition: all 0.2s ease;
  user-select: none;
}

.edit-btn:hover {
  background-color: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
  border-color: rgba(37, 99, 235, 0.18);
}

.edit-btn:active {
  background-color: rgba(37, 99, 235, 0.14);
}

.edit-btn svg {
  flex-shrink: 0;
}

.delete-section {
  display: flex;
  justify-content: flex-end;
  padding: 18px 20px;
  border: 1px solid rgba(239, 68, 68, 0.14);
  background: linear-gradient(180deg, #fff8f8 0%, #fffdfd 100%);
  border-radius: 16px;
}

.delete-btn {
  padding: 10px 22px;
  background: transparent;
  border: 1px solid #ef4444;
  color: #ef4444;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 999px;
  transition: all 0.2s ease;
  user-select: none;
}

.delete-btn:hover {
  background-color: #fef0f0;
  border-color: #dc2626;
  color: #dc2626;
  box-shadow: 0 8px 18px rgba(239, 68, 68, 0.12);
}

.delete-btn:active {
  background-color: #fee2e2;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .security-view {
    padding: 22px;
    border-radius: 14px;
  }

  .info-row {
    padding: 16px 18px;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .info-right {
    width: 100%;
    justify-content: flex-end;
  }

  .delete-section {
    justify-content: center;
  }

  .delete-btn {
    width: 100%;
  }
}

/* 确认弹窗统一样式 */
.delete-confirm-dialog :deep(.el-dialog) {
  margin-top: 15vh !important;
  border-radius: 18px;
  overflow: hidden;
}

.delete-confirm-dialog :deep(.el-dialog__header) {
  padding-bottom: 4px;
}

.delete-confirm-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.confirm-content {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.confirm-icon {
  flex-shrink: 0;
  margin-top: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.warning-icon {
  color: #e6a23c;
}

.error-icon {
  color: #f56c6c;
}

.confirm-message {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  flex: 1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 第一次确认：正常顺序（取消左，确定注销右） */
.dialog-footer-normal {
  flex-direction: row;
}

/* 第二次确认：反转顺序（确定注销左，取消右） */
.dialog-footer-reverse {
  flex-direction: row-reverse;
}
</style>
