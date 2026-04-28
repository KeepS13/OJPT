<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import LoginDialog from '@/components/auth/LoginDialog.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useAuth } from '@/hooks/useAuth'
import { getProblemDetailByNo, getProblemSampleTestCases, runProblemCode, submitProblemCode } from '@/api/problem'
import { renderMarkdown } from '@/utils/markdown'
import type { SupportedLanguage } from '@/constants/languageTemplates'
import { getProblemDefaultTestCases, getProblemTemplate } from '@/utils/problemPresets'

const route = useRoute()
const router = useRouter()

const routeProblemNo = computed(() => route.params.problemNo)

type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
type ProblemStatus = 'UNSOLVED' | 'ATTEMPTED' | 'SOLVED'

interface ProblemTagVO {
  id: string
  name: string
  type?: string | null
}

interface ProblemDetailVO {
  id: string
  problemNo: number
  title: string
  difficulty: Difficulty
  statementMd?: string | null
  submitCount?: number | null
  acceptedCount?: number | null
  acceptanceRate?: number | null
  tags?: ProblemTagVO[] | null
  status?: ProblemStatus | null
  timeLimitMs?: number | null
  memoryLimitKb?: number | null
}

const problemDetail = ref<ProblemDetailVO | null>(null)
const loadingProblem = ref(false)

const statementHtml = computed(() =>
  problemDetail.value?.statementMd ? renderMarkdown(problemDetail.value.statementMd) : '',
)

const languages: SupportedLanguage[] = ['C/C++', 'Java', 'Python3']
const activeLanguage = ref<SupportedLanguage>('C/C++')

const resolveTemplate = (lang: SupportedLanguage, problem: ProblemDetailVO | null): string => {
  return getProblemTemplate(problem, lang)
}

const code = ref<string>(resolveTemplate(activeLanguage.value, null))

// 代码编辑器：行号与滚动同步
const codeEditorRef = ref<HTMLTextAreaElement | null>(null)
const lineNumbersRef = ref<HTMLElement | null>(null)

const lineNumbers = computed(() =>
  Array.from({ length: Math.max(1, code.value.split('\n').length) }, (_, i) => i + 1),
)

const syncCodeScroll = () => {
  if (!codeEditorRef.value || !lineNumbersRef.value) return
  lineNumbersRef.value.scrollTop = codeEditorRef.value.scrollTop
}

const resetCodeToDefault = () => {
  code.value = resolveTemplate(activeLanguage.value, problemDetail.value)
  nextTick(() => {
    if (codeEditorRef.value) codeEditorRef.value.scrollTop = 0
    if (lineNumbersRef.value) lineNumbersRef.value.scrollTop = 0
  })
}

const isRunning = ref(false)
const isSubmitting = ref(false)
const isTiming = ref(false)
const elapsedSeconds = ref(0)
let timerHandle: ReturnType<typeof setInterval> | null = null

const formattedElapsed = computed(() => {
  const minutes = Math.floor(elapsedSeconds.value / 60)
  const seconds = elapsedSeconds.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const stopTimer = () => {
  if (timerHandle) {
    clearInterval(timerHandle)
    timerHandle = null
  }
  isTiming.value = false
}

const handleToggleTimer = () => {
  if (isTiming.value) {
    stopTimer()
    ElMessage.success('计时已停止')
    return
  }

  isTiming.value = true
  timerHandle = setInterval(() => {
    elapsedSeconds.value += 1
  }, 1000)
  ElMessage.success('计时已开始')
}

const handleRunCode = async () => {
  if (!isAuthed.value) {
    showLogin.value = true
    return
  }

  if (!code.value.trim()) {
    ElMessage.warning('请先输入代码')
    return
  }

  const cases = visibleTestCases.value.map((item) => ({
    inputText: item.inputText,
    expectedOutput: item.outputText,
  }))
  if (!cases.length) {
    ElMessage.warning('请先添加测试用例')
    return
  }

  try {
    isRunning.value = true
    const result = await runProblemCode({
      language: activeLanguage.value,
      sourceCode: code.value,
      timeLimitMs: problemDetail.value?.timeLimitMs,
      memoryLimitKb: problemDetail.value?.memoryLimitKb,
      cases,
    })
    const targetCases = activeTestMode.value === 'sample' ? sampleTestCases.value : testCases.value
    result.data.caseResults.forEach((caseResult) => {
      const target = targetCases[caseResult.caseIndex]
      if (!target) return
      target.outputText = caseResult.actualOutput ?? ''
      target.status = caseResult.status === 'AC' ? 'success' : 'failed'
    })
    const allPassed = result.data.caseResults.every((item) => item.status === 'AC')
    if (allPassed) {
      ElMessage.success('运行通过')
    } else {
      ElMessage.warning('运行完成，存在未通过用例')
    }
  } finally {
    isRunning.value = false
  }
}

const handleSubmitCode = async () => {
  if (!isAuthed.value) {
    showLogin.value = true
    return
  }

  if (!code.value.trim()) {
    ElMessage.warning('请先输入代码')
    return
  }

  try {
    isSubmitting.value = true
    await new Promise((resolve) => window.setTimeout(resolve, 300))
    ElMessage.success('提交入口已恢复，当前环境未接入判题接口')
  } finally {
    isSubmitting.value = false
  }
}

const handleSubmitCodeReal = async () => {
  if (!isAuthed.value) {
    showLogin.value = true
    return
  }

  if (!code.value.trim()) {
    ElMessage.warning('请输入代码')
    return
  }

  try {
    isSubmitting.value = true
    const result = await submitProblemCode(Number(routeProblemNo.value), {
      language: activeLanguage.value,
      sourceCode: code.value,
    })
    ElMessage.success(result.data.message || '代码已提交')
    await loadProblemDetail()
  } finally {
    isSubmitting.value = false
  }
}

watch(
  activeLanguage,
  async (nextLang, prevLang) => {
    if (!prevLang || nextLang === prevLang) return

    const prevTemplate = resolveTemplate(prevLang, problemDetail.value).trim()
    const currentCode = code.value.trim()

    if (currentCode === prevTemplate) {
      code.value = resolveTemplate(nextLang, problemDetail.value)
      await nextTick(() => {
        if (codeEditorRef.value) codeEditorRef.value.scrollTop = 0
        if (lineNumbersRef.value) lineNumbersRef.value.scrollTop = 0
      })
      return
    }

    try {
      await ElMessageBox.confirm(
        '切换语言将使用该语言的默认模板，当前代码不会自动保存。确认切换吗？',
        '切换语言',
        {
          confirmButtonText: '切换',
          cancelButtonText: '取消',
          type: 'warning',
        },
      )
      code.value = resolveTemplate(nextLang, problemDetail.value)
      await nextTick(() => {
        if (codeEditorRef.value) codeEditorRef.value.scrollTop = 0
        if (lineNumbersRef.value) lineNumbersRef.value.scrollTop = 0
      })
    } catch {
      activeLanguage.value = prevLang
    }
  },
)

const loadProblemDetail = async () => {
  loadingProblem.value = true
  try {
    const [res, sampleCaseRes] = await Promise.all([
      getProblemDetailByNo(String(routeProblemNo.value)),
      getProblemSampleTestCases(String(routeProblemNo.value)),
    ])
    const body: any = res.data
    const data = body && typeof body === 'object' && 'data' in body ? body.data : body
    problemDetail.value = data as ProblemDetailVO
    code.value = resolveTemplate(activeLanguage.value, problemDetail.value)
    sampleTestCases.value = (
      sampleCaseRes.data?.length ? sampleCaseRes.data : getProblemDefaultTestCases(problemDetail.value)
    ).map(toTestCase)
    testCases.value = [
      {
        id: 1,
        name: 'Case 1',
        inputText: sampleTestCases.value[0]?.inputText || '',
        outputText: '',
        status: 'default',
      },
    ]
    activeTestMode.value = sampleTestCases.value.length ? 'sample' : 'custom'
    activeSampleCaseIndex.value = 0
    activeCaseIndex.value = 0
  } catch (e) {
    ElMessage.error('加载题目详情失败，请稍后重试')
  } finally {
    loadingProblem.value = false
  }
}

// 测试用例数据结构与状态
type TestCaseStatus = 'default' | 'success' | 'failed'

interface TestCase {
  id: number
  name: string
  inputText: string
  outputText: string
  explanation?: string | null
  status?: TestCaseStatus
}

const toTestCase = (
  item: {
    name?: string
    inputText?: string
    outputText?: string
    expectedOutput?: string
    explanation?: string | null
  },
  index: number,
): TestCase => ({
  id: index + 1,
  name: item.name || `Case ${index + 1}`,
  inputText: item.inputText || '',
  outputText: item.outputText ?? item.expectedOutput ?? '',
  explanation: item.explanation ?? null,
  status: 'default',
})

const testCases = ref<TestCase[]>(
  getProblemDefaultTestCases({ problemNo: 1 }).map(toTestCase),
)

const sampleTestCases = ref<TestCase[]>([])
const activeTestMode = ref<'sample' | 'custom'>('sample')
const activeSampleCaseIndex = ref(0)
const activeCaseIndex = ref(0)
const hoveredCaseIndex = ref(-1)

const visibleTestCases = computed(() =>
  activeTestMode.value === 'sample' ? sampleTestCases.value : testCases.value,
)

const activeTestCase = computed(() => {
  if (!visibleTestCases.value.length) return null
  const currentIndex = activeTestMode.value === 'sample' ? activeSampleCaseIndex.value : activeCaseIndex.value
  const index =
    currentIndex >= 0 && currentIndex < visibleTestCases.value.length
      ? currentIndex
      : 0
  return visibleTestCases.value[index]
})

const onSelectTestCase = (index: number) => {
  if (index < 0 || index >= visibleTestCases.value.length) return
  if (activeTestMode.value === 'sample') {
    activeSampleCaseIndex.value = index
    return
  }
  activeCaseIndex.value = index
}

const onAddTestCase = () => {
  if (activeTestMode.value !== 'custom') {
    activeTestMode.value = 'custom'
  }
  // 最多 8 个测试用例
  if (testCases.value.length >= 8) return
  const current = activeTestCase.value ?? testCases.value[0]
  const nextId = (testCases.value[testCases.value.length - 1]?.id ?? 0) + 1
  const nextIndex = testCases.value.length

  const baseInputText = current?.inputText ?? ''
  const baseOutputText = current?.outputText ?? ''

  testCases.value.push({
    id: nextId,
    name: `Case ${nextIndex + 1}`,
    inputText: baseInputText,
    outputText: baseOutputText,
    status: 'default',
  })

  activeCaseIndex.value = nextIndex
}

const onDeleteTestCase = (index: number, event: MouseEvent) => {
  if (activeTestMode.value !== 'custom') return
  event.stopPropagation()
  if (testCases.value.length <= 1) return
  if (index < 0 || index >= testCases.value.length) return

  testCases.value.splice(index, 1)

  if (!testCases.value.length) {
    activeCaseIndex.value = 0
    hoveredCaseIndex.value = -1
    return
  }

  if (activeCaseIndex.value > index) {
    activeCaseIndex.value -= 1
  } else if (activeCaseIndex.value >= testCases.value.length) {
    activeCaseIndex.value = testCases.value.length - 1
  }

  if (hoveredCaseIndex.value === index) hoveredCaseIndex.value = -1
  else if (hoveredCaseIndex.value > index) hoveredCaseIndex.value -= 1
}

const updateTestCaseInput = (caseIndex: number, value: string) => {
  if (activeTestMode.value !== 'custom') return
  if (!testCases.value.length) return
  if (caseIndex < 0 || caseIndex >= testCases.value.length) return
  const target = testCases.value[caseIndex]
  if (!target) return
  target.inputText = value
}

const updateTestCaseOutput = (caseIndex: number, value: string) => {
  if (activeTestMode.value !== 'custom') return
  if (!testCases.value.length) return
  if (caseIndex < 0 || caseIndex >= testCases.value.length) return
  const target = testCases.value[caseIndex]
  if (!target) return
  target.outputText = value
}

// 左右面板分隔条（可拖拽调整宽度）
const solveMainRef = ref<HTMLElement | null>(null)
const statementPanelRef = ref<HTMLElement | null>(null)
const editorPanelRef = ref<HTMLElement | null>(null)
const editorBodyRef = ref<HTMLElement | null>(null)
const splitterWidth = 8
const leftSplitRatio = ref(0.45) // 左侧题面所占比例
const isResizing = ref(false)

const storageKey = computed(() => `OJPT.solve.leftSplitRatio`)

// 右侧编辑器内部上下分隔（代码区 / 测试用例区）
const editorSplitterHeight = 8
const splitRatio = ref(0.6) // 上方代码区域所占比例
const editorStorageKey = computed(() => `OJPT.solve.editorSplitRatio`)
const editorPanelHeight = ref(0) // 右侧面板当前高度，用于按比例计算上下区域

const applyInitialWidth = async () => {
  await nextTick()
  // 1) localStorage
  const raw = localStorage.getItem(storageKey.value)
  const saved = raw ? Number(raw) : NaN
  if (Number.isFinite(saved) && saved > 0 && saved < 1) {
    leftSplitRatio.value = saved
    return
  }

  // 2) fallback：用当前布局的实际宽度当初始值
  const containerWidth = solveMainRef.value?.getBoundingClientRect().width ?? 0
  const currentLeft = statementPanelRef.value?.getBoundingClientRect().width ?? 0
  const available = Math.max(0, containerWidth - splitterWidth)
  if (available > 0 && currentLeft > 0) {
    leftSplitRatio.value = Math.min(1, Math.max(0, currentLeft / available))
  }
}

const getEditorPanelHeight = () => editorPanelHeight.value

const refreshEditorPanelHeight = () => {
  if (!editorPanelRef.value) {
    editorPanelHeight.value = 0
    return
  }
  const rect = editorPanelRef.value.getBoundingClientRect()
  editorPanelHeight.value = rect.height
}

const applyInitialEditorHeight = async () => {
  await nextTick()
  const raw = localStorage.getItem(editorStorageKey.value)
  const saved = raw ? Number(raw) : NaN
  if (Number.isFinite(saved) && saved > 0 && saved < 1) {
    splitRatio.value = saved
    return
  }
  splitRatio.value = 0.6
}

let containerLeft = 0
let availableWidth = 0
let containerTop = 0
let availableHeight = 0

const onSplitterPointerMove = (e: PointerEvent) => {
  if (!isResizing.value) return
  if (!availableWidth) return
  const pointerOffset = e.clientX - containerLeft
  const nextRatio = pointerOffset / availableWidth
  if (Number.isFinite(nextRatio)) {
    // 这里做 0..1 限制，避免出现负宽度导致 grid 计算异常
    leftSplitRatio.value = Math.min(1, Math.max(0, nextRatio))
  }
}

const isEditorResizing = ref(false)

const onInnerSplitterPointerMove = (e: PointerEvent) => {
  if (!isEditorResizing.value) return
  if (!availableHeight) return
  const pointerOffset = e.clientY - containerTop
  const nextRatio = pointerOffset / availableHeight
  if (Number.isFinite(nextRatio)) {
    splitRatio.value = nextRatio
  }
}

const stopResizing = () => {
  if (!isResizing.value) return
  isResizing.value = false
  localStorage.setItem(storageKey.value, String(leftSplitRatio.value))
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('pointermove', onSplitterPointerMove)
  window.removeEventListener('pointerup', stopResizing)
  window.removeEventListener('pointercancel', stopResizing)
}

const stopEditorResizing = () => {
  if (!isEditorResizing.value) return
  isEditorResizing.value = false
  localStorage.setItem(editorStorageKey.value, String(splitRatio.value))
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('pointermove', onInnerSplitterPointerMove)
  window.removeEventListener('pointerup', stopEditorResizing)
  window.removeEventListener('pointercancel', stopEditorResizing)
}

const onSplitterPointerDown = (e: PointerEvent) => {
  if (!solveMainRef.value) return
  isResizing.value = true
  const rect = solveMainRef.value.getBoundingClientRect()
  containerLeft = rect.left
  availableWidth = Math.max(0, rect.width - splitterWidth)

  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  e.preventDefault()
  window.addEventListener('pointermove', onSplitterPointerMove)
  window.addEventListener('pointerup', stopResizing)
  window.addEventListener('pointercancel', stopResizing)
}

const onInnerSplitterPointerDown = (e: PointerEvent) => {
  if (!editorPanelRef.value) return
  isEditorResizing.value = true
  const rect = editorPanelRef.value.getBoundingClientRect()
  containerTop = rect.top
  availableHeight = Math.max(0, rect.height - editorSplitterHeight)

  document.body.style.cursor = 'row-resize'
  document.body.style.userSelect = 'none'
  e.preventDefault()
  window.addEventListener('pointermove', onInnerSplitterPointerMove)
  window.addEventListener('pointerup', stopEditorResizing)
  window.addEventListener('pointercancel', stopEditorResizing)
}

const onWindowResize = () => {
  // 左右面板按 ratio 控制宽度，不在此修改；这里只是同步右侧可用高度
  refreshEditorPanelHeight()
}

let editorResizeObserver: ResizeObserver | null = null

onMounted(async () => {
  await loadProblemDetail()
  await applyInitialWidth()
  await applyInitialEditorHeight()
  refreshEditorPanelHeight()

  if (window.ResizeObserver && editorPanelRef.value) {
    editorResizeObserver = new ResizeObserver(() => {
      refreshEditorPanelHeight()
    })
    editorResizeObserver.observe(editorPanelRef.value)
  }

  window.addEventListener('resize', onWindowResize)
})

onBeforeUnmount(() => {
  stopResizing()
  stopEditorResizing()
  stopTimer()
  window.removeEventListener('resize', onWindowResize)
  if (editorResizeObserver && editorPanelRef.value) {
    editorResizeObserver.unobserve(editorPanelRef.value)
    editorResizeObserver.disconnect()
    editorResizeObserver = null
  }
})

const solveMainStyle = computed(() => {
  const ratio = Number.isFinite(leftSplitRatio.value) ? leftSplitRatio.value : 0.45
  return {
    gridTemplateColumns: `minmax(0, ${ratio}fr) ${splitterWidth}px minmax(0, ${1 - ratio}fr)`,
  }
})

const editorTopStyle = computed(() => {
  const height = getEditorPanelHeight()
  if (!height) return {}
  const available = Math.max(0, height - editorSplitterHeight)
  const ratio = Number.isFinite(splitRatio.value) ? splitRatio.value : 0.6
  const topHeight = Math.max(0, available * ratio)
  return {
    height: `${topHeight}px`,
  }
})

const editorBottomStyle = computed(() => {
  const height = getEditorPanelHeight()
  if (!height) return {}
  const available = Math.max(0, height - editorSplitterHeight)
  const ratio = Number.isFinite(splitRatio.value) ? splitRatio.value : 0.6
  const topHeight = Math.max(0, available * ratio)
  const bottomHeight = Math.max(0, available - topHeight)
  return {
    height: `${bottomHeight}px`,
  }
})

const handleBackToSet = () => {
  router.push('/problemset')
}

// 用户信息与下拉菜单（与顶部导航保持一致的行为）
const showLogin = ref(false)
const showMenu = ref(false)
const { isAuthed, user, logout } = useAuth()

const displayName = computed(() => {
  return user.value?.username || user.value?.email || ''
})

const roleDisplay = computed(() => {
  const code = user.value?.roleType
  if (!code) return null
  const map: Record<string, { tag: string }> = {
    USER: { tag: '用户' },
    ADMIN: { tag: '管理员' },
  }
  return map[code] ?? { tag: code }
})

let hideTimer: ReturnType<typeof setTimeout> | null = null

const openLogin = () => {
  showLogin.value = true
}

const onUserEnter = () => {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
  showMenu.value = true
}

const onUserLeave = () => {
  if (hideTimer) {
    clearTimeout(hideTimer)
  }
  hideTimer = setTimeout(() => {
    showMenu.value = false
  }, 120)
}

const handleLogout = () => {
  logout()
  showMenu.value = false
  router.push('/')
}
</script>

<template>
  <div class="solve-layout">
    <header class="solve-header">
      <button type="button" class="back-btn" @click="handleBackToSet">
        返回题库
      </button>
      <div class="title-block">
        <div class="title-row">
          <div class="title-main">
            <h1 class="problem-title">
              {{ problemDetail?.problemNo ? 'P' + String(problemDetail.problemNo).padStart(4, '0') + '. ' : '' }}{{ problemDetail?.title || '两数之和' }}
            </h1>
            <span v-if="problemDetail?.status === 'SOLVED'" class="badge badge--solved">
              已解答
            </span>
            <span
              class="difficulty-badge"
              :class="{
                'difficulty-badge--easy': problemDetail?.difficulty === 'EASY' || !problemDetail,
                'difficulty-badge--medium': problemDetail?.difficulty === 'MEDIUM',
                'difficulty-badge--hard': problemDetail?.difficulty === 'HARD',
              }"
            >
              {{
                problemDetail?.difficulty === 'MEDIUM'
                  ? '中等'
                  : problemDetail?.difficulty === 'HARD'
                    ? '困难'
                    : '简单'
              }}
            </span>
          </div>
          <div class="solve-user-area">
            <button
              v-if="!isAuthed"
              type="button"
              class="login-btn"
              @click="openLogin"
            >
              登录
            </button>
            <div
              v-else
              class="nav-user"
              @mouseenter="onUserEnter"
              @mouseleave="onUserLeave"
            >
              <UserAvatar
                :name="displayName"
                :size="32"
                :role-type="user?.roleType"
                :avatar="user?.avatar || null"
                class="nav-avatar"
              />
              <transition name="fade">
                <div v-if="showMenu" class="user-menu">
                  <div class="user-menu__header">
                    <div class="user-menu__name">{{ displayName || '用户' }}</div>
                    <div class="user-menu__role" v-if="roleDisplay">
                      <span class="role-badge" :class="`role-badge--${user?.roleType?.toLowerCase()}`">
                        {{ roleDisplay.tag }}
                      </span>
                    </div>
                  </div>
                  <div class="user-menu__body">
                    <RouterLink to="/profile" class="user-menu__item">个人中心</RouterLink>
                    <RouterLink
                      v-if="user?.roles?.includes('ADMIN')"
                      to="/admin"
                      class="user-menu__item"
                    >
                      管理员控制台
                    </RouterLink>
                    <RouterLink to="/profile/security" class="user-menu__item">账号安全</RouterLink>
                  </div>
                  <div class="user-menu__footer">
                    <button type="button" class="user-menu__logout" @click="handleLogout">
                      退出登录
                    </button>
                  </div>
                </div>
              </transition>
            </div>
          </div>
        </div>
      </div>
    </header>

    <main ref="solveMainRef" class="solve-main" :class="{ 'solve-main--resizing': isResizing }" :style="solveMainStyle">
      <section ref="statementPanelRef" class="statement-panel">
        <div class="statement-scroll">
          <h2 class="section-title">题目描述</h2>
          <div class="meta-row">
            <span class="meta-item">
              通过率
              {{
                problemDetail && problemDetail.acceptanceRate != null
                  ? ` ${Number(problemDetail.acceptanceRate).toFixed(1)}%`
                  : ' --'
              }}
            </span>
            <span class="meta-dot" />
            <span class="meta-item">
              {{
                problemDetail && problemDetail.submitCount != null
                  ? `${problemDetail.submitCount} 次提交`
                  : ' -- 次提交'
              }}
            </span>
          </div>
          <div v-if="problemDetail?.tags?.length" class="tag-row">
            <span
              v-for="tag in problemDetail.tags"
              :key="tag.id"
              class="tag-pill"
            >
              {{ tag.name }}
            </span>
          </div>
          <div v-else class="tag-row">
            <span class="tag-pill">数组</span>
            <span class="tag-pill">哈希表</span>
          </div>

          <div v-if="loadingProblem" class="statement-loading">
            正在加载题目详情...
          </div>
          <template v-else>
            <div v-if="problemDetail?.statementMd" class="markdown-body" v-html="statementHtml" />
            <template v-else>
              <p class="paragraph">
                给定一个整数数组
                <code>nums</code>
                和一个整数目标值
                <code>target</code>
                ，请你在该数组中找出和为目标值
                <code>target</code>
                的那&nbsp;<strong>两个</strong>&nbsp;整数，并返回它们的数组下标。
              </p>
              <p class="paragraph">
                你可以假设每种输入只会对应一个答案。但是，数组中同一个元素在答案里不能重复出现。
              </p>
              <p class="paragraph">
                你可以按任意顺序返回答案。
              </p>

              <h3 class="sub-section-title">示例 1：</h3>
              <pre class="code-block">
输入：nums = [2,7,11,15], target = 9
输出：[0,1]
解释：因为 nums[0] + nums[1] == 9 ，返回 [0, 1]。</pre
              >

              <h3 class="sub-section-title">示例 2：</h3>
              <pre class="code-block">
输入：nums = [3,2,4], target = 6
输出：[1,2]</pre
              >

              <h3 class="sub-section-title">示例 3：</h3>
              <pre class="code-block">
输入：nums = [3,3], target = 6
输出：[0,1]</pre
              >

              <h3 class="sub-section-title">提示：</h3>
              <ul class="hint-list">
                <li>2 &lt;= nums.length &lt;= 10<sup>4</sup></li>
                <li>-10<sup>9</sup> &lt;= nums[i] &lt;= 10<sup>9</sup></li>
                <li>-10<sup>9</sup> &lt;= target &lt;= 10<sup>9</sup></li>
                <li>只会存在一个有效答案</li>
              </ul>
            </template>
          </template>
        </div>
      </section>

      <div
        class="pane-splitter"
        role="separator"
        aria-label="调整左右面板宽度"
        @pointerdown="onSplitterPointerDown"
      />

      <section ref="editorPanelRef" class="editor-panel" :class="{ 'editor-panel--resizing': isEditorResizing }">
        <div class="editor-card editor-card--top" :style="editorTopStyle">
          <header class="editor-header">
            <div class="editor-header-left">
              <span class="editor-label">代码</span>
              <select v-model="activeLanguage" class="language-select">
                <option v-for="lang in languages" :key="lang" :value="lang">
                  {{ lang }}
                </option>
              </select>
            </div>
            <div class="editor-header-right">
              <button
                type="button"
                class="btn-secondary"
                data-testid="run-code-button"
                :disabled="isRunning"
                @click="handleRunCode"
              >
                <span class="run-btn-content">
                  <svg class="run-btn-icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M8 6.5v11l9-5.5-9-5.5Z" fill="currentColor" />
                  </svg>
                  <span>{{ isRunning ? '运行中...' : '运行' }}</span>
                </span>
              </button>
              <button
                type="button"
                class="btn-primary"
                data-testid="submit-code-button"
                :disabled="isSubmitting"
                @click="handleSubmitCodeReal"
              >
                {{ isSubmitting ? '提交中...' : '提交' }}
              </button>
              <div class="icon-hint-wrapper">
                <button
                  type="button"
                  class="btn-icon-refresh"
                  aria-label="还原到默认的代码模版"
                  title="还原到默认的代码模版"
                  @click="resetCodeToDefault"
                >
                  <svg class="btn-icon-refresh__glyph" viewBox="0 0 24 24" aria-hidden="true">
                    <path
                      d="M20 5v5h-5"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.85"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                    <path
                      d="M20 10a8 8 0 1 0 2.2 5.5"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.85"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </header>

          <section ref="editorBodyRef" class="editor-body">
            <div class="code-editor-wrapper">
              <div ref="lineNumbersRef" class="code-line-numbers">
                <span
                  v-for="line in lineNumbers"
                  :key="line"
                  class="code-line-number"
                >
                  {{ line }}
                </span>
              </div>
              <textarea
                ref="codeEditorRef"
                v-model="code"
                class="code-editor"
                spellcheck="false"
                @scroll="syncCodeScroll"
              />
            </div>
          </section>
        </div>

        <div
          class="editor-inner-splitter"
          role="separator"
          aria-label="调整代码编辑区与测试用例区域高度"
          @pointerdown="onInnerSplitterPointerDown"
        />

        <div class="editor-card editor-card--bottom" :style="editorBottomStyle">
          <footer class="editor-footer">
            <header class="testcase-header">
              <span class="editor-footer-title">测试用例</span>
              <div class="testcase-mode-switch">
                <button
                  type="button"
                  class="testcase-mode-btn"
                  :class="{ 'testcase-mode-btn--active': activeTestMode === 'sample' }"
                  @click="activeTestMode = 'sample'"
                >
                  题目样例
                </button>
                <button
                  type="button"
                  class="testcase-mode-btn"
                  :class="{ 'testcase-mode-btn--active': activeTestMode === 'custom' }"
                  @click="activeTestMode = 'custom'"
                >
                  我的测试
                </button>
              </div>
              <div class="testcase-tabs">
                <button
                  v-for="(item, index) in visibleTestCases"
                  :key="item.id"
                  type="button"
                  class="testcase-tab"
                  :class="{
                    'testcase-tab--active': index === activeCaseIndex,
                  }"
                  @click="onSelectTestCase(index)"
                  @mouseenter="activeTestMode === 'custom' ? hoveredCaseIndex = index : null"
                  @mouseleave="activeTestMode === 'custom' ? hoveredCaseIndex = -1 : null"
                >
                  <span
                    class="testcase-icon"
                    :class="[
                      `testcase-icon--${item.status || 'default'}`,
                    ]"
                    aria-hidden="true"
                  />
                  <span class="testcase-name">Case {{ index + 1 }}</span>
                  <span
                    v-if="activeTestMode === 'custom' && hoveredCaseIndex === index && testCases.length > 1"
                    class="testcase-delete-btn"
                    role="button"
                    tabindex="0"
                    aria-label="删除测试用例"
                    @pointerdown.stop
                    @click.stop="onDeleteTestCase(index, $event)"
                    @keydown.enter.stop.prevent="onDeleteTestCase(index, $event as unknown as MouseEvent)"
                    @keydown.space.stop.prevent="onDeleteTestCase(index, $event as unknown as MouseEvent)"
                  >
                    ×
                  </span>
                </button>
                <button
                  v-if="activeTestMode === 'custom' && testCases.length < 8"
                  type="button"
                  class="testcase-tab testcase-tab--add"
                  @click="onAddTestCase"
                  aria-label="新增测试用例（最多 8 个）"
                >
                  +
                </button>
              </div>
            </header>
            <div
              v-if="
                visibleTestCases.length &&
                activeTestCase &&
                activeCaseIndex >= 0 &&
                activeCaseIndex < visibleTestCases.length
              "
              class="testcase-body"
            >
              <div class="testcase-row testcase-row--stacked">
                <span class="testcase-label">输入</span>
                <textarea
                  :value="activeTestCase.inputText"
                  @input="updateTestCaseInput(activeCaseIndex, ($event.target as HTMLTextAreaElement).value)"
                  class="testcase-textarea"
                  spellcheck="false"
                  rows="4"
                  :readonly="activeTestMode === 'sample'"
                />
              </div>
              <div class="testcase-row testcase-row--stacked">
                <span class="testcase-label">输出</span>
                <textarea
                  :value="activeTestCase.outputText"
                  @input="updateTestCaseOutput(activeCaseIndex, ($event.target as HTMLTextAreaElement).value)"
                  class="testcase-textarea"
                  spellcheck="false"
                  rows="2"
                  :readonly="activeTestMode === 'sample'"
                />
              </div>
              <div v-if="activeTestMode === 'sample' && activeTestCase.explanation" class="testcase-explanation">
                {{ activeTestCase.explanation }}
              </div>
            </div>
            <div v-else class="testcase-empty">
              暂无测试用例，请点击上方 <span class="testcase-empty__accent">+</span> 添加。
            </div>
          </footer>
        </div>
      </section>
    </main>
    <LoginDialog v-model="showLogin" />
  </div>
</template>

<style scoped>
.solve-layout {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 8px 16px 16px;
  box-sizing: border-box;
}

.solve-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 4px 12px;
  border-bottom: 1px solid #e5e7eb;
}

.back-btn {
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  color: #4b5563;
  cursor: pointer;
}

.back-btn:hover {
  background-color: #f9fafb;
}

.title-block {
  flex: 1;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.title-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.problem-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
}

.badge--solved {
  background-color: #ecfdf3;
  color: #16a34a;
}

.solve-user-area {
  display: flex;
  align-items: center;
}

.login-btn {
  padding: 6px 16px;
  border-radius: 999px;
  border: 1px solid #2563eb;
  color: #2563eb;
  font-size: 13px;
  background-color: #ffffff;
  cursor: pointer;
}

.login-btn:hover {
  background-color: #eff6ff;
}

.nav-avatar {
  cursor: pointer;
}

.nav-user {
  position: relative;
}

.difficulty-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
}

.difficulty-badge--easy {
  background-color: #ecfdf3;
  color: #16a34a;
}

.meta-row {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
}

.meta-dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background-color: #d1d5db;
}

.tag-row {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-pill {
  padding: 2px 8px;
  border-radius: 999px;
  background-color: #f3f4f6;
  color: #4b5563;
  font-size: 11px;
}

.user-menu {
  position: absolute;
  right: 0;
  top: 44px;
  width: 220px;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  padding: 10px 0 8px;
  z-index: 20;
}

.user-menu__header {
  padding: 0 16px 6px;
  border-bottom: 1px solid #f3f4f6;
}

.user-menu__name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.user-menu__role {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, #ff6bb3, #ff3c7d);
  user-select: none;
}

.role-badge--user {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}

.role-badge--admin {
  background: linear-gradient(135deg, #fb7185, #ef4444);
}

.user-menu__body {
  padding: 4px 0;
}

.user-menu__item {
  display: block;
  padding: 6px 16px;
  font-size: 13px;
  color: #374151;
}

.user-menu__item:hover {
  background-color: #f9fafb;
}

.user-menu__footer {
  padding: 4px 16px 0;
  border-top: 1px solid #f3f4f6;
  margin-top: 4px;
}

.user-menu__logout {
  width: 100%;
  border: 0;
  background: transparent;
  color: #ef4444;
  font-size: 13px;
  text-align: left;
  padding: 6px 0;
  cursor: pointer;
}

.user-menu__logout:hover {
  color: #b91c1c;
}

.solve-main {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) 12px minmax(0, 1.3fr);
  grid-auto-rows: minmax(0, 1fr);
  gap: 0;
  padding-top: 10px;
  min-height: 0;
}

.solve-main--resizing {
  cursor: col-resize;
}

.statement-panel {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.editor-panel {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
  height: 100%;
}

.editor-card {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.editor-card--top {
  overflow: hidden;
}

.editor-card--bottom {
  flex: 0 0 auto;
  min-height: 0;
  overflow: hidden;
}

.pane-splitter {
  position: relative;
  width: 8px;
  cursor: col-resize;
  touch-action: none;
  user-select: none;
  border-radius: 10px;
  transition: background-color 0.12s ease;
}

.pane-splitter::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 10px;
  transform: translateX(-50%);
  width: 2px;
  height: calc(100% - 20px);
  border-radius: 999px;
  background-color: #e5e7eb;
  transition: background-color 0.12s ease;
}

.pane-splitter::after {
  content: '';
  position: absolute;
  inset: 6px 0;
  border-radius: 10px;
  background: rgba(34, 197, 94, 0.08);
  opacity: 0;
  transition: opacity 0.12s ease;
  pointer-events: none;
}

.pane-splitter:hover::before,
.solve-main--resizing .pane-splitter::before {
  background-color: #a7f3d0;
}

.pane-splitter:hover::after,
.solve-main--resizing .pane-splitter::after {
  opacity: 1;
}

.statement-scroll {
  flex: 1;
  min-height: 0;
  padding: 12px 16px 16px;
  overflow-y: auto;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.paragraph {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.7;
  margin: 0 0 8px 0;
}

code {
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  background-color: #f3f4f6;
  padding: 0 4px;
  border-radius: 4px;
  font-size: 12px;
}

.sub-section-title {
  font-size: 13px;
  font-weight: 600;
  margin: 10px 0 4px;
}

.code-block {
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  background-color: #f9fafb;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  white-space: pre-wrap;
  margin: 0 0 8px 0;
}

.hint-list {
  padding-left: 18px;
  font-size: 13px;
  color: #4b5563;
}

.markdown-body {
  font-size: 13px;
  color: #374151;
  line-height: 1.7;
}

:deep(.markdown-body > h1:first-child) {
  display: none;
}

:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3),
:deep(.markdown-body h4) {
  font-weight: 600;
  margin: 12px 0 8px;
}

:deep(.markdown-body p) {
  margin: 0 0 8px 0;
}

:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 20px;
  margin: 0 0 8px 0;
}

:deep(.markdown-body li) {
  margin: 2px 0;
}

:deep(.markdown-body > ul) {
  list-style: none;
  padding: 8px 14px;
  margin: 10px 0 14px;
  border-left: 4px solid #d4d4d8;
  background-color: #f9fafb;
  border-radius: 4px;
}

:deep(.markdown-body code) {
  background-color: #f3f4f6;
  padding: 0 4px;
  border-radius: 4px;
  font-size: 12px;
}

:deep(.markdown-body pre) {
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  background-color: #f9fafb;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  white-space: pre-wrap;
  margin: 0 0 8px 0;
  overflow-x: auto;
}

:deep(.markdown-body pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  font-size: inherit;
  color: inherit;
}

.editor-header {
  padding: 8px 12px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.editor-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.editor-label {
  font-size: 13px;
  font-weight: 500;
}

.language-select {
  font-size: 13px;
  font-weight: 500;
  color: #111827;
  min-width: 124px;
  padding: 6px 32px 6px 14px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  outline: none;
  appearance: none;
  cursor: pointer;
  line-height: 1.25;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
  transition: border-color 120ms ease, box-shadow 120ms ease, background-color 120ms ease;
  /* 右侧下拉箭头（SVG） */
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12' fill='none'%3E%3Cpath d='M3 4.5L6 7.5L9 4.5' stroke='%236b7280' stroke-width='1.4' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px 12px;
}

.language-select:hover {
  border-color: #c7d2fe;
  background-color: #fbfbff;
  box-shadow: 0 2px 6px rgba(99, 102, 241, 0.08);
}

.language-select:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.15), 0 1px 2px rgba(15, 23, 42, 0.06);
}

.language-select option {
  font-weight: 400;
  color: #111827;
}

.editor-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-secondary {
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid transparent;
}

.btn-primary {
  border-radius: 999px;
  padding: 5px 14px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid #16a34a;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #ffffff;
  box-shadow: 0 6px 14px rgba(34, 197, 94, 0.18);
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.02);
}

.btn-primary:disabled,
.btn-secondary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.icon-hint-wrapper {
  position: relative;
  display: inline-flex;
}

.btn-secondary {
  background-color: #f3f4f6;
  color: #374151;
  border-color: #e5e7eb;
}

.btn-secondary:hover {
  background-color: #e5e7eb;
}

.run-btn-content {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.run-btn-icon {
  width: 13px;
  height: 13px;
  display: block;
}

.btn-icon-refresh {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid #dbe3ee;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  color: #475569;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease,
    transform 0.16s ease,
    box-shadow 0.16s ease;
}

.btn-icon-refresh:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.12);
}

.btn-icon-refresh:active {
  transform: translateY(0);
}

.btn-icon-refresh:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(59, 130, 246, 0.16),
    0 8px 16px rgba(37, 99, 235, 0.12);
}

.btn-icon-refresh__glyph {
  width: 16px;
  height: 16px;
  display: block;
}

.editor-body {
  flex: 1;
  padding: 8px 12px;
  min-height: 0;
  overflow: hidden;
}

.editor-inner-splitter {
  position: relative;
  height: 8px;
  cursor: row-resize;
  touch-action: none;
  user-select: none;
  border-radius: 10px;
  margin: 0;
}

.editor-inner-splitter::before {
  content: '';
  position: absolute;
  left: 6px;
  right: 6px;
  top: 3px;
  height: 2px;
  border-radius: 999px;
  background-color: #e5e7eb;
  transition: background-color 0.12s ease;
}

.editor-inner-splitter::after {
  content: '';
  position: absolute;
  inset: 0 6px;
  border-radius: 10px;
  background: rgba(34, 197, 94, 0.08);
  opacity: 0;
  transition: opacity 0.12s ease;
  pointer-events: none;
}

.editor-inner-splitter:hover::before,
.editor-panel--resizing .editor-inner-splitter::before {
  background-color: #a7f3d0;
}

.editor-inner-splitter:hover::after,
.editor-panel--resizing .editor-inner-splitter::after {
  opacity: 1;
}

.code-editor-wrapper {
  display: flex;
  flex: 1;
  min-height: 0;
  height: 100%;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  overflow: hidden;
  box-sizing: border-box;
}

.code-line-numbers {
  width: 40px;
  padding: 10px 4px;
  box-sizing: border-box;
  background-color: #f9fafb;
  border-right: 1px solid #e5e7eb;
  text-align: right;
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
  height: 100%;
  overflow: hidden;
}

.code-line-number {
  display: block;
}

.code-editor {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
  border: none;
  padding: 10px 12px;
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  resize: none;
  outline: none;
  overflow: auto;
  box-sizing: border-box;
}

.code-editor:focus {
  outline: none;
}

.editor-footer {
  padding: 0 12px 10px;
  border-top: 1px solid #e5e7eb;
  font-size: 12px;
  color: #4b5563;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.testcase-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 6px 0 6px;
  margin-bottom: 6px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.editor-footer-title {
  font-size: 12px;
  font-weight: 500;
  color: #111827;
}

.testcase-mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.testcase-mode-btn {
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #4b5563;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 11px;
  cursor: pointer;
}

.testcase-mode-btn--active {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #2563eb;
  font-weight: 600;
}

.testcase-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.testcase-tab {
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background-color: #f9fafb;
  padding: 3px 10px;
  font-size: 11px;
  color: #4b5563;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  position: relative;
}

.testcase-delete-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 16px;
  height: 16px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: #ef4444;
  color: #ffffff;
  border: 1px solid #ffffff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  z-index: 2;
  user-select: none;
  transition: transform 0.15s ease, background-color 0.15s ease;
}

.testcase-delete-btn:hover {
  background-color: #dc2626;
  transform: scale(1.08);
}

.testcase-delete-btn:active {
  transform: scale(0.94);
}

.testcase-delete-btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.35);
}

.testcase-tab--active {
  background-color: #dcfce7;
  border-color: #bbf7d0;
  color: #15803d;
  font-weight: 500;
}

.testcase-tab--add {
  padding-inline: 8px;
  font-size: 14px;
  line-height: 1;
  border-style: dashed;
  background-color: #ffffff;
}

.testcase-tab--add:hover {
  background-color: #f9fafb;
}

.testcase-icon {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background-color: #d1d5db;
}

.testcase-icon--default {
  background-color: #d1d5db;
}

.testcase-icon--success {
  background-color: #22c55e;
}

.testcase-icon--failed {
  background-color: #ef4444;
}

.testcase-name {
  max-width: 80px;
  text-overflow: ellipsis;
  overflow: hidden;
}

.testcase-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.testcase-row {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr);
  align-items: start;
  gap: 8px;
}

.testcase-row--stacked {
  grid-template-columns: 60px minmax(0, 1fr);
}

.editor-footer > .testcase-tab {
  margin-right: 4px;
}

.testcase-label {
  font-size: 12px;
  color: #6b7280;
}

.testcase-input {
  border-radius: 999px;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  padding: 6px 10px;
  font-size: 12px;
  color: #111827;
  width: 100%;
  box-sizing: border-box;
  outline: none;
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.testcase-input:focus {
  border-color: #22c55e;
  box-shadow: 0 0 0 1px rgba(34, 197, 94, 0.3);
  background-color: #ffffff;
}

.testcase-textarea {
  border-radius: 12px;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  padding: 8px 10px;
  font-size: 12px;
  color: #111827;
  width: 100%;
  min-height: 56px;
  box-sizing: border-box;
  outline: none;
  resize: vertical;
  line-height: 1.5;
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.testcase-textarea:focus {
  border-color: #22c55e;
  box-shadow: 0 0 0 1px rgba(34, 197, 94, 0.3);
  background-color: #ffffff;
}

.testcase-textarea[readonly] {
  background-color: #f8fafc;
  color: #475569;
  cursor: default;
}

.testcase-value {
  border-radius: 999px;
  background-color: #f9fafb;
  border: 1px solid #e5e7eb;
  padding: 6px 10px;
  font-size: 12px;
  color: #111827;
}

.testcase-empty {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.testcase-empty__accent {
  font-weight: 600;
  color: #16a34a;
}

.testcase-explanation {
  font-size: 12px;
  color: #64748b;
  line-height: 1.7;
  background: #f8fafc;
  border: 1px dashed #dbe3ee;
  border-radius: 10px;
  padding: 8px 10px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
