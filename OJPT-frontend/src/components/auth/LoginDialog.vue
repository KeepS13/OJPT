<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Form as VForm, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'
import { login, register, requestPasswordReset } from '@/api/auth'
import { useAuth } from '@/hooks/useAuth'

interface LoginForm {
  account: string
  password: string
}

interface RegisterForm extends LoginForm {
  nickname: string
  gender: 1 | 2 | ''
}

type AuthForm = LoginForm | RegisterForm
type LoginTab = 'email' | 'phone'
type AuthMode = 'login' | 'register'

defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const auth = useAuth()
const loading = ref(false)
const resetLoading = ref(false)
const showResetDialog = ref(false)
const resetAccount = ref('')
const authMode = ref<AuthMode>('login')
const activeTab = ref<LoginTab>('email')
const autoLoggingIn = computed(() => auth.authInitializing && !auth.isAuthed)

const passwordInputRef = ref<InstanceType<typeof import('element-plus').ElInput> | null>(null)

const emailDomains = ['qq.com', '163.com', '126.com', 'gmail.com', 'outlook.com', 'hotmail.com', 'icloud.com']
const accountInput = ref('')
const showEmailSuggest = ref(false)
const emailSuggestList = ref<string[]>([])
const emailFocused = ref(false)
const selectedIndex = ref(-1)
const isNavigatingSuggest = ref(false)

const isRegister = computed(() => authMode.value === 'register')
const formKey = computed(() => `${authMode.value}-${activeTab.value}`)

const initialValues = computed<RegisterForm>(() => ({
  account: '',
  password: '',
  nickname: '',
  gender: '',
}))

const baseAccountSchema = computed(() =>
  activeTab.value === 'email'
    ? yup
        .string()
        .required('请输入邮箱')
        .email('请输入正确的邮箱地址')
    : yup
        .string()
        .required('请输入手机号')
        .matches(/^1[3-9]\d{9}$/, '请输入正确的手机号')
)

const validationSchema = computed(() => {
  const shape = {
    account: baseAccountSchema.value,
    password: isRegister.value
      ? yup.string().required('请输入密码').min(6, '密码至少 6 位').max(64, '密码不能超过 64 位')
      : yup.string().required('请输入密码'),
  }

  if (!isRegister.value) {
    return yup.object(shape)
  }

  return yup.object({
    ...shape,
    nickname: yup.string().trim().required('请输入昵称').max(30, '昵称不能超过 30 个字符'),
    gender: yup
      .number()
      .transform((value, originalValue) => (originalValue === '' ? undefined : value))
      .oneOf([1, 2], '请选择性别')
      .required('请选择性别'),
  })
})

const accountPlaceholder = computed(() =>
  activeTab.value === 'email' ? '请输入邮箱' : '请输入手机号'
)

const tabLabels = computed(() => ({
  email: authMode.value === 'login' ? '邮箱登录' : '邮箱注册',
  phone: authMode.value === 'login' ? '手机号登录' : '手机号注册',
}))

const submitText = computed(() => {
  if (autoLoggingIn.value) return '正在自动登录...'
  return authMode.value === 'login' ? '登录' : '注册并登录'
})

const formatRemaining = (secs: number) => {
  const s = Math.max(0, Math.floor(secs))
  const days = Math.floor(s / 86400)
  const hours = Math.floor((s % 86400) / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  const seconds = s % 60
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${days}天 ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

const getErrorMessage = (error: unknown) => {
  const resp = (error as {
    response?: { data?: { message?: string; data?: { remainingSeconds?: number } }; status?: number }
    message?: string
  })?.response

  const remainingSeconds = resp?.data?.data?.remainingSeconds
  if (resp?.status === 403 && remainingSeconds !== undefined) {
    return `账号已被封禁，剩余：${formatRemaining(remainingSeconds)}`
  }

  const serverMsg = resp?.data?.message
  if (resp?.status === 401) {
    return serverMsg && /bad credentials/i.test(serverMsg)
      ? '用户名或密码错误'
      : serverMsg || '用户名或密码错误'
  }

  if (serverMsg) return serverMsg
  if (resp?.status === undefined) return '请求失败：网络异常，请检查连接后重试'
  return (error as { message?: string })?.message || '请求失败：请稍后重试'
}

const onSubmit = async (values: AuthForm, setFieldValue?: (field: string, value: unknown) => void) => {
  if (loading.value) return
  loading.value = true
  try {
    const { account, password } = values
    const response = isRegister.value
      ? await register({
          account,
          password,
          nickname: (values as RegisterForm).nickname.trim(),
          gender: (values as RegisterForm).gender as 1 | 2,
        })
      : await login({ account, password })

    auth.loginSuccess(response.data)
    ElMessage.success(isRegister.value ? '注册成功' : '登录成功')
    setFieldValue?.('password', '')
    emit('update:modelValue', false)
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    loading.value = false
  }
}

const openResetDialog = () => {
  resetAccount.value = accountInput.value.trim()
  showResetDialog.value = true
}

const submitPasswordReset = async () => {
  const account = resetAccount.value.trim()
  if (!account) {
    ElMessage.warning('请输入用户名、邮箱或手机号')
    return
  }

  resetLoading.value = true
  try {
    await requestPasswordReset({ account })
    ElMessage.success('已通知管理员，请等待重置')
    showResetDialog.value = false
  } catch (error: unknown) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    resetLoading.value = false
  }
}

const resetSuggest = () => {
  showEmailSuggest.value = false
  emailSuggestList.value = []
  selectedIndex.value = -1
  isNavigatingSuggest.value = false
}

const updateEmailSuggestions = (value: string, field: { onChange: (val: string) => void }) => {
  if (activeTab.value === 'phone') {
    field.onChange(value)
    accountInput.value = value
    resetSuggest()
    return
  }

  field.onChange(value)
  accountInput.value = value

  if (!emailFocused.value) {
    resetSuggest()
    return
  }

  const [localPart, domainPart = ''] = value.split('@')
  if (!localPart || /\s/.test(value)) {
    resetSuggest()
    return
  }

  const lowerDomain = domainPart.toLowerCase()
  const list = emailDomains
    .filter((d) => d.startsWith(lowerDomain))
    .map((d) => `${localPart}@${d}`)

  emailSuggestList.value = list
  showEmailSuggest.value = list.length > 0
  if (!isNavigatingSuggest.value) {
    selectedIndex.value = -1
  }
}

const applyEmailSuggestion = (suggest: string, field: { onChange: (value: string) => void }) => {
  field.onChange(suggest)
  accountInput.value = suggest
  resetSuggest()
  emailFocused.value = false
  setTimeout(() => {
    passwordInputRef.value?.focus()
  }, 100)
}

const onAccountFocus = (field: { value?: string; onChange: (val: string) => void }) => {
  emailFocused.value = true
  const current = field.value || accountInput.value
  if (activeTab.value === 'email' && current) {
    updateEmailSuggestions(current, field)
  }
}

const onAccountBlur = () => {
  setTimeout(() => {
    if (!isNavigatingSuggest.value) {
      emailFocused.value = false
      resetSuggest()
    }
  }, 150)
}

const onAccountClear = (
  field: { onChange: (val: string) => void },
  setFieldValue?: (field: string, value: unknown) => void
) => {
  field.onChange('')
  accountInput.value = ''
  resetSuggest()
  setFieldValue?.('password', '')
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

const onAccountKeydown = (
  event: KeyboardEvent,
  field: { value?: string; onChange: (val: string) => void }
) => {
  if (activeTab.value !== 'email' || !showEmailSuggest.value || emailSuggestList.value.length === 0) {
    return
  }

  if (event.key === 'Tab' && !event.shiftKey) {
    event.preventDefault()
    if (selectedIndex.value === -1) {
      selectedIndex.value = 0
      isNavigatingSuggest.value = true
    } else {
      moveDown()
    }
    return
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    selectedIndex.value === -1 ? (selectedIndex.value = 0) : moveDown()
    isNavigatingSuggest.value = true
    return
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    moveUp()
    isNavigatingSuggest.value = true
    return
  }

  if (event.key === 'Enter' && selectedIndex.value >= 0 && selectedIndex.value < emailSuggestList.value.length) {
    event.preventDefault()
    const selected = emailSuggestList.value[selectedIndex.value]
    if (selected) applyEmailSuggestion(selected, field)
    return
  }

  if (event.key === 'Escape') {
    event.preventDefault()
    resetSuggest()
  }
}

const switchTab = (tab: LoginTab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  accountInput.value = ''
  resetSuggest()
}

const switchMode = (mode: AuthMode) => {
  if (authMode.value === mode) return
  authMode.value = mode
  accountInput.value = ''
  resetSuggest()
}

const close = () => {
  if (!loading.value) {
    emit('update:modelValue', false)
  }
}

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
          {{ tabLabels.email }}
        </button>
        <button
          class="tab"
          :class="{ active: activeTab === 'phone' }"
          type="button"
          @click="switchTab('phone')"
        >
          {{ tabLabels.phone }}
        </button>
      </div>

      <VForm
        :key="formKey"
        :initial-values="initialValues"
        :validation-schema="validationSchema"
        v-slot="{ handleSubmit: submitForm, setFieldValue }"
      >
        <el-form
          label-position="top"
          class="login-form"
          @submit.prevent="submitForm((values) => onSubmit(values as AuthForm, setFieldValue))"
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
                :placeholder="isRegister ? '请输入密码（至少 6 位）' : '请输入密码'"
                :autocomplete="isRegister ? 'new-password' : 'current-password'"
                @keyup.enter="submitForm((values) => onSubmit(values as AuthForm, setFieldValue))"
              />
            </Field>
          </el-form-item>
          <ErrorMessage name="password" v-slot="{ message }">
            <div class="field-error">{{ message }}</div>
          </ErrorMessage>

          <template v-if="isRegister">
            <el-form-item>
              <Field name="nickname" v-slot="{ field }">
                <el-input
                  :model-value="field.value"
                  data-testid="register-nickname-input"
                  @update:model-value="field.onChange"
                  placeholder="请输入昵称"
                  maxlength="30"
                  clearable
                />
              </Field>
            </el-form-item>
            <ErrorMessage name="nickname" v-slot="{ message }">
              <div class="field-error">{{ message }}</div>
            </ErrorMessage>

            <div class="register-row">
              <Field name="gender" v-slot="{ field }">
                <el-radio-group
                  :model-value="field.value"
                  data-testid="register-gender-radio"
                  class="gender-group"
                  @update:model-value="field.onChange"
                >
                  <el-radio-button :value="1">男</el-radio-button>
                  <el-radio-button :value="2">女</el-radio-button>
                </el-radio-group>
              </Field>
              <ErrorMessage name="gender" v-slot="{ message }">
                <div class="field-error">{{ message }}</div>
              </ErrorMessage>
            </div>
          </template>

          <el-form-item>
            <el-button
              type="primary"
              data-testid="login-submit-button"
              @click="submitForm((values) => onSubmit(values as AuthForm, setFieldValue))"
              :loading="loading || autoLoggingIn"
              :disabled="autoLoggingIn"
              class="submit-btn"
              block
            >
              {{ submitText }}
            </el-button>
          </el-form-item>

          <div class="action-links" :class="{ 'action-links--single': isRegister }">
            <a
              v-if="!isRegister"
              href="javascript:void(0)"
              data-testid="switch-register-link"
              @click="switchMode('register')"
            >
              注册
            </a>
            <a
              v-else
              href="javascript:void(0)"
              data-testid="switch-login-link"
              @click="switchMode('login')"
            >
              已有账号？登录
            </a>
            <a v-if="!isRegister" href="javascript:void(0)" @click="openResetDialog">忘记密码</a>
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

  <el-dialog v-model="showResetDialog" title="忘记密码" width="420px">
    <el-form label-position="top" @submit.prevent="submitPasswordReset">
      <el-form-item label="用户名、邮箱或手机号">
        <el-input
          v-model="resetAccount"
          data-testid="password-reset-account-input"
          placeholder="请输入用户名、邮箱或手机号"
          clearable
          @keyup.enter="submitPasswordReset"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showResetDialog = false">取消</el-button>
      <el-button
        type="primary"
        :loading="resetLoading"
        data-testid="password-reset-submit-button"
        @click="submitPasswordReset"
      >
        提交
      </el-button>
    </template>
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

.login-form :deep(.el-input__wrapper),
.login-form :deep(.el-date-editor.el-input__wrapper) {
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

.register-row {
  margin-top: 14px;
}

.gender-group {
  width: 100%;
}

.gender-group :deep(.el-radio-button) {
  width: 50%;
}

.gender-group :deep(.el-radio-button__inner) {
  width: 100%;
  height: 42px;
  line-height: 20px;
  padding: 10px 12px;
}

.login-form :deep(.el-date-editor) {
  width: 100%;
}

.action-links {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin: 4px 6px 6px;
}

.action-links--single {
  justify-content: center;
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

@media (max-width: 480px) {
  .login-dialog {
    width: calc(100vw - 24px) !important;
  }

  .register-row {
    grid-template-columns: 1fr;
  }
}
</style>
