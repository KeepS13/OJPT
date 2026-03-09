<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Form as VForm, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'
import { login } from '@/api/auth'
import { useAuth } from '@/hooks/useAuth'

interface LoginForm {
  account: string
  password: string
}

defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const auth = useAuth()
const loading = ref(false)
const autoLoggingIn = computed(() => auth.authInitializing && !auth.isAuthed)

// 密码输入框的引用
const passwordInputRef = ref<InstanceType<typeof import('element-plus').ElInput> | null>(null)

// 登录方式：email 或 phone
type LoginTab = 'email' | 'phone'
const activeTab = ref<LoginTab>('email')

// 邮箱自动补全
const emailDomains = ['qq.com', '163.com', '126.com', 'gmail.com', 'outlook.com', 'hotmail.com', 'icloud.com']
const accountInput = ref('')
const showEmailSuggest = ref(false)
const emailSuggestList = ref<string[]>([])
const emailFocused = ref(false)
const selectedIndex = ref(-1) // 当前选中的下拉选项索引，-1 表示未选中
const isNavigatingSuggest = ref(false) // 是否正在导航下拉框

const loginSchema = computed(() =>
  activeTab.value === 'email'
    ? yup.object({
        account: yup
          .string()
          .required('请输入邮箱')
          .email('请输入正确的邮箱地址'),
        password: yup.string().required('请输入密码'),
      })
    : yup.object({
        account: yup
          .string()
          .required('请输入手机号')
          .matches(/^1[3-9]\d{9}$/, '请输入正确的手机号'),
        password: yup.string().required('请输入密码'),
      })
)

const accountPlaceholder = computed(() =>
  activeTab.value === 'email' ? '请输入邮箱' : '请输入手机号'
)

const initialValues: LoginForm = {
  account: '',
  password: '',
}

const formatRemaining = (secs: number) => {
  const s = Math.max(0, Math.floor(secs))
  const days = Math.floor(s / 86400)
  const hours = Math.floor((s % 86400) / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  const seconds = s % 60
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${days}天 ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

const onSubmit = async (values: LoginForm, setFieldValue?: (field: string, value: unknown) => void) => {
  if (loading.value) return
  loading.value = true
  try {
    const { data } = await login(values)
    auth.loginSuccess(data)
    ElMessage.success('登录成功')
    // 清除密码字段
    if (setFieldValue) {
      setFieldValue('password', '')
    }
    emit('update:modelValue', false)
  } catch (error: unknown) {
    const resp = (error as {
      response?: { data?: { message?: string; data?: { remainingSeconds?: number } }; status?: number }
      message?: string
    })?.response

    const remainingSeconds = resp?.data?.data?.remainingSeconds
    const banMsg =
      resp?.status === 403 && remainingSeconds !== undefined
        ? `账号已被封禁，剩余：${formatRemaining(remainingSeconds)}`
        : null

    const serverMsg = resp?.data?.message

    // 统一登录错误提示：把常见英文/无 message 场景映射为更友好的中文
    const normalized401Msg =
      resp?.status === 401
        ? serverMsg && /bad credentials/i.test(serverMsg)
          ? '用户名或密码错误'
          : serverMsg || '用户名或密码错误'
        : null

    const msg =
      banMsg ??
      normalized401Msg ??
      serverMsg ??
      (resp?.status === undefined
        ? '登录失败：网络异常，请检查连接后重试'
        : (error as { message?: string })?.message) ??
      '登录失败：请稍后重试'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const updateEmailSuggestions = (value: string, field: { onChange: (val: string) => void }) => {
  // 手机号登录模式下，不提供邮箱补全
  if (activeTab.value === 'phone') {
    field.onChange(value)
    accountInput.value = value
    showEmailSuggest.value = false
    emailSuggestList.value = []
    selectedIndex.value = -1
    isNavigatingSuggest.value = false
    return
  }

  field.onChange(value)
  accountInput.value = value

  if (!emailFocused.value) {
    showEmailSuggest.value = false
    emailSuggestList.value = []
    selectedIndex.value = -1
    isNavigatingSuggest.value = false
    return
  }

  const [localPart, domainPart = ''] = value.split('@')
  if (!localPart || /\s/.test(value)) {
    showEmailSuggest.value = false
    emailSuggestList.value = []
    selectedIndex.value = -1
    isNavigatingSuggest.value = false
    return
  }

  const lowerDomain = domainPart.toLowerCase()
  const list = emailDomains
    .filter((d) => d.startsWith(lowerDomain))
    .map((d) => `${localPart}@${d}`)

  emailSuggestList.value = list
  showEmailSuggest.value = list.length > 0
  // 重置选中索引
  if (!isNavigatingSuggest.value) {
    selectedIndex.value = -1
  }
}

const applyEmailSuggestion = (suggest: string, field: { onChange: (value: string) => void }) => {
  field.onChange(suggest)
  accountInput.value = suggest
  showEmailSuggest.value = false
  selectedIndex.value = -1
  isNavigatingSuggest.value = false
  emailFocused.value = false
  // 自动聚焦到密码输入框
  setTimeout(() => {
    passwordInputRef.value?.focus()
  }, 100)
}

const onAccountFocus = (field: { value?: string; onChange: (val: string) => void }) => {
  // 重新获得焦点时，重新启用建议功能
  emailFocused.value = true
  const current = field.value || accountInput.value
  if (activeTab.value === 'email' && current) {
    // 如果有输入内容，立即显示建议
    updateEmailSuggestions(current, field)
  }
}

const onAccountBlur = () => {
  // 延迟隐藏，给点击建议项留时间
  setTimeout(() => {
    // 如果不在导航建议状态，说明用户点击了别处，立即隐藏
    if (!isNavigatingSuggest.value) {
      emailFocused.value = false
      showEmailSuggest.value = false
      emailSuggestList.value = []
      selectedIndex.value = -1
    }
  }, 150)
}

const onAccountClear = (
  field: { onChange: (val: string) => void },
  setFieldValue?: (field: string, value: unknown) => void
) => {
  field.onChange('')
  accountInput.value = ''
  showEmailSuggest.value = false
  emailSuggestList.value = []
  selectedIndex.value = -1
  isNavigatingSuggest.value = false
  // 同时清空密码字段
  if (setFieldValue) {
    setFieldValue('password', '')
  }
}

const onAccountKeydown = (
  event: KeyboardEvent,
  field: { value?: string; onChange: (val: string) => void }
) => {
  // 只在邮箱登录模式下且有下拉框时处理
  if (activeTab.value !== 'email' || !showEmailSuggest.value || emailSuggestList.value.length === 0) {
    // 无下拉框时，Tab 键正常行为（跳转到下一个输入框）
    if (event.key === 'Tab' && !event.shiftKey) {
      return // 允许默认行为
    }
    return
  }

  const key = event.key

  // Tab 键：进入下拉框（选中第一个选项）
  if (key === 'Tab' && !event.shiftKey) {
    event.preventDefault()
    if (selectedIndex.value === -1) {
      // 从输入框进入下拉框，选中第一个
      selectedIndex.value = 0
      isNavigatingSuggest.value = true
    } else {
      // 在下拉框内，Tab 等同于方向键下
      moveDown()
    }
    return
  }

  // 方向键下：选择下一个选项
  if (key === 'ArrowDown') {
    event.preventDefault()
    if (selectedIndex.value === -1) {
      selectedIndex.value = 0
    } else {
      moveDown()
    }
    isNavigatingSuggest.value = true
    return
  }

  // 方向键上：选择上一个选项
  if (key === 'ArrowUp') {
    event.preventDefault()
    moveUp()
    isNavigatingSuggest.value = true
    return
  }

  // Enter 键：应用选中的建议
  if (key === 'Enter' && selectedIndex.value >= 0 && selectedIndex.value < emailSuggestList.value.length) {
    event.preventDefault()
    const selected = emailSuggestList.value[selectedIndex.value]
    if (selected) {
      applyEmailSuggestion(selected, field)
      // applyEmailSuggestion 内部已经处理了聚焦，这里不需要重复
    }
    return
  }

  // Escape 键：关闭下拉框
  if (key === 'Escape') {
    event.preventDefault()
    showEmailSuggest.value = false
    selectedIndex.value = -1
    isNavigatingSuggest.value = false
    return
  }
}

const moveDown = () => {
  if (emailSuggestList.value.length === 0) return
  selectedIndex.value = (selectedIndex.value + 1) % emailSuggestList.value.length
}

const moveUp = () => {
  if (emailSuggestList.value.length === 0) return
  if (selectedIndex.value === -1) {
    selectedIndex.value = emailSuggestList.value.length - 1
  } else {
    selectedIndex.value = selectedIndex.value <= 0
      ? emailSuggestList.value.length - 1
      : selectedIndex.value - 1
  }
}

const switchTab = (tab: LoginTab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  showEmailSuggest.value = false
  emailSuggestList.value = []
  selectedIndex.value = -1
  isNavigatingSuggest.value = false
}

const close = () => {
  if (!loading.value) {
    emit('update:modelValue', false)
  }
}

// 自动登录完成且已是登录状态时，自动关闭弹窗
watch(
  () => auth.isAuthed,
  (val) => {
    if (val) {
      emit('update:modelValue', false)
    }
  }
)
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    width="420px"
    :close-on-click-modal="false"
    :show-close="!loading"
    @close="close"
    class="login-dialog"
  >
    <template #title>
      <div class="dialog-title">
        <img alt="OJPT logo" class="title-logo" src="@/assets/logo.svg" />
        <span class="title-text">OJPT</span>
      </div>
    </template>

    <div class="login-body">
      <div class="login-tabs">
        <button
          class="tab"
          :class="{ active: activeTab === 'email' }"
          type="button"
          @click="switchTab('email')"
        >
          邮箱登录
        </button>
        <button
          class="tab"
          :class="{ active: activeTab === 'phone' }"
          type="button"
          @click="switchTab('phone')"
        >
          手机号登录
        </button>
      </div>

      <VForm
        :initial-values="initialValues"
        :validation-schema="loginSchema"
        v-slot="{ handleSubmit: submitForm, setFieldValue }"
      >
        <el-form
          label-position="top"
          class="login-form"
          @submit.prevent="submitForm((values) => onSubmit(values as LoginForm, setFieldValue))"
        >
          <el-form-item>
            <Field name="account" v-slot="{ field }">
              <div class="account-input-wrapper">
                <el-input
                  :model-value="field.value"
                  :disabled="autoLoggingIn"
                  data-testid="login-account-input"
                  @update:model-value="(val: string) => updateEmailSuggestions(val, field)"
                  :placeholder="accountPlaceholder"
                  :autocomplete="activeTab === 'email' ? 'email' : 'tel'"
                  @focus="onAccountFocus(field)"
                  @blur="onAccountBlur"
                  @keydown="(e: KeyboardEvent) => onAccountKeydown(e, field)"
                  clearable
                  @clear="() => onAccountClear(field, setFieldValue)"
                />
                <ul
                  v-if="showEmailSuggest && emailSuggestList.length"
                  class="email-suggest"
                  @mouseenter="isNavigatingSuggest = true"
                  @mouseleave="isNavigatingSuggest = false"
                >
                  <li
                    v-for="(item, index) in emailSuggestList"
                    :key="item"
                    class="email-suggest__item"
                    :class="{ 'email-suggest__item--selected': selectedIndex === index }"
                    @mousedown.prevent="applyEmailSuggestion(item, field)"
                    @mouseenter="selectedIndex = index"
                  >
                    {{ item }}
                  </li>
                </ul>
              </div>
            </Field>
          </el-form-item>
          <ErrorMessage name="account" v-slot="{ message }">
            <div class="field-error">{{ message }}</div>
          </ErrorMessage>

          <el-form-item>
            <Field name="password" v-slot="{ field }">
              <el-input
                ref="passwordInputRef"
                :model-value="field.value"
                :disabled="autoLoggingIn"
                data-testid="login-password-input"
                @update:model-value="field.onChange"
                type="password"
                show-password
                placeholder="请输入密码"
                autocomplete="current-password"
                @keyup.enter="submitForm((values) => onSubmit(values as LoginForm, setFieldValue))"
              />
            </Field>
          </el-form-item>
          <ErrorMessage name="password" v-slot="{ message }">
            <div class="field-error">{{ message }}</div>
          </ErrorMessage>

          <el-form-item>
            <el-button
              type="primary"
              data-testid="login-submit-button"
              @click="submitForm((values) => onSubmit(values as LoginForm, setFieldValue))"
              :loading="loading || autoLoggingIn"
              :disabled="autoLoggingIn"
              class="submit-btn"
              block
            >
              {{ autoLoggingIn ? '正在自动登录…' : '登录' }}
            </el-button>
          </el-form-item>

        <div class="action-links">
          <a href="javascript:void(0)">注册</a>
          <a href="javascript:void(0)">忘记密码</a>
        </div>
        </el-form>
      </VForm>

      <p class="agreement">
        登录或注册即代表你同意
        <a href="javascript:void(0)">《用户协议》</a>
        和
        <a href="javascript:void(0)">《隐私协议》</a>
      </p>
    </div>
  </el-dialog>
</template>

<style scoped>
.login-dialog :deep(.el-dialog__body) {
  padding: 18px 24px 22px;
  background: #f9fafb;
  border-radius: 12px;
}

.dialog-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 6px 0 2px;
}

.title-logo {
  width: 28px;
  height: 28px;
}

.title-text {
  font-weight: 600;
  font-size: 20px;
  letter-spacing: 0.08em;
}

.login-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.login-tabs {
  display: flex;
  gap: 8px;
}

.tab {
  flex: 1;
  height: 36px;
  border-radius: 18px;
  border: 0;
  font-size: 14px;
  cursor: pointer;
  font-weight: 600;
}

.tab.active {
  background-color: #111827;
  color: #ffffff;
}

.tab.disabled {
  background-color: #f3f4f6;
  color: #9ca3af;
  cursor: not-allowed;
}

.login-form {
  margin-top: 2px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 2px;
  font-weight: 600;
  color: #374151;
}

.login-form :deep(.el-form-item) {
  margin: 14px 0 4px;
}

.login-form :deep(.el-input__wrapper) {
  height: 42px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #d1d5db inset;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2563eb inset, 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.submit-btn {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  font-weight: 700;
}

.account-input-wrapper {
  position: relative;
  width: 100%;
}

.email-suggest {
  position: absolute;
  left: 0;
  right: 0;
  top: 44px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.12);
  padding: 4px 0;
  z-index: 15;
  max-height: 180px;
  overflow-y: auto;
}

.email-suggest__item {
  padding: 4px 12px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
}

.email-suggest__item:hover {
  background: #f3f4f6;
}

.email-suggest__item--selected {
  background: #e5e7eb;
}

.action-links {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin: 4px 6px 6px;
}

.action-links a {
  color: #2563eb;
}

.field-error {
  margin: 2px 0 4px 2px;
  font-size: 12px;
  color: #f56c6c;
}

.agreement {
  margin: 6px 0 0;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
}

.agreement a {
  color: #2563eb;
}
</style>

