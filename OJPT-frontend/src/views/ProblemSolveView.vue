<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import LoginDialog from '@/components/auth/LoginDialog.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ProblemTimer from '@/components/problem/ProblemTimer.vue'
import { useAuth } from '@/hooks/useAuth'
import { getProblemDetailByNo } from '@/api/problem'
import { createSubmission } from '@/api/submission'
import { renderMarkdown } from '@/utils/markdown'
import { defaultLanguageTemplates, type SupportedLanguage } from '@/constants/languageTemplates'

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
const submitting = ref(false)

const statementHtml = computed(() =>
  problemDetail.value?.statementMd ? renderMarkdown(problemDetail.value.statementMd) : '',
)

const languages: SupportedLanguage[] = ['C/C++', 'Java', 'Python3']
const activeLanguage = ref<SupportedLanguage>('C/C++')

const resolveTemplate = (lang: SupportedLanguage, problem: ProblemDetailVO | null): string => {
  return defaultLanguageTemplates[lang]?.template ?? ''
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

const runCodeWithTestCases = () => {
  // TODO: 接入后端/沙箱执行。这里先做交互占位，避免按钮无响应。
  console.log('run: 调用测试用例运行（占位）')
}

const loadProblemDetail = async () => {
  loadingProblem.value = true
  try {
    const res = await getProblemDetailByNo(String(routeProblemNo.value))
    const body: any = res.data
    const data = body && typeof body === 'object' && 'data' in body ? body.data : body
    problemDetail.value = data as ProblemDetailVO
  } catch (e) {
    ElMessage.error('加载题目详情失败，请稍后重试')
  } finally {
    loadingProblem.value = false
  }
}

// 测试用例数据结构与状态
type TestCaseStatus = 'default' | 'success' | 'failed'

interface TestCaseInput {
  label: string
  value: string
}

interface TestCase {
  id: number
  name: string
  inputs: TestCaseInput[]
  status?: TestCaseStatus
}

const testCases = ref<TestCase[]>([
  {
    id: 1,
    name: 'Case 1',
    inputs: [
      { label: 'nums', value: '[2,7,11,15]' },
      { label: 'target', value: '9' },
    ],
    status: 'default',
  },
  {
    id: 2,
    name: 'Case 2',
    inputs: [
      { label: 'nums', value: '[3,2,4]' },
      { label: 'target', value: '6' },
    ],
    status: 'default',
  },
  {
    id: 3,
    name: 'Case 3',
    inputs: [
      { label: 'nums', value: '[3,3]' },
      { label: 'target', value: '6' },
    ],
    status: 'default',
  },
])

const activeCaseIndex = ref(0)
const hoveredCaseIndex = ref(-1)

const activeTestCase = computed(() => {
  if (!testCases.value.length) return null
  const index =
    activeCaseIndex.value >= 0 && activeCaseIndex.value < testCases.value.length
      ? activeCaseIndex.value
      : 0
  return testCases.value[index]
})

const onSelectTestCase = (index: number) => {
  if (index < 0 || index >= testCases.value.length) return
  activeCaseIndex.value = index
}

const onAddTestCase = () => {
  // 最多 8 个测试用例
  if (testCases.value.length >= 8) return
  const current = activeTestCase.value ?? testCases.value[0]
  const nextId = (testCases.value[testCases.value.length - 1]?.id ?? 0) + 1
  const nextIndex = testCases.value.length

  const baseInputs =
    current?.inputs?.length
      ? current.inputs.map((item) => ({
          label: item.label,
          value: item.value,
        }))
      : [
          { label: 'nums', value: '[]' },
          { label: 'target', value: '0' },
        ]

  testCases.value.push({
    id: nextId,
    name: `Case ${nextIndex + 1}`,
    inputs: baseInputs,
    status: 'default',
  })

  activeCaseIndex.value = nextIndex
}

const onDeleteTestCase = (index: number, event: MouseEvent) => {
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

const updateTestCaseInput = (caseIndex: number, inputIndex: number, value: string) => {
  if (!testCases.value.length) return
  if (caseIndex < 0 || caseIndex >= testCases.value.length) return
  const target = testCases.value[caseIndex]
  if (!target || !target.inputs?.length) return
  if (inputIndex < 0 || inputIndex >= target.inputs.length) return
  const targetInput = target.inputs[inputIndex]
  if (!targetInput) return
  targetInput.value = value
}

// 图标按钮悬停提示（下拉面板）
type IconHintKind = 'reset' | 'run' | null
const iconHint = ref<IconHintKind>(null)
let iconHintHideTimeout: number | null = null

const onIconHintEnter = (kind: Exclude<IconHintKind, null>) => {
  if (iconHintHideTimeout !== null) {
    window.clearTimeout(iconHintHideTimeout)
    iconHintHideTimeout = null
  }
  iconHint.value = kind
}

const onIconHintLeave = () => {
  if (iconHintHideTimeout !== null) {
    window.clearTimeout(iconHintHideTimeout)
  }
  iconHintHideTimeout = window.setTimeout(() => {
    iconHint.value = null
    iconHintHideTimeout = null
  }, 80)
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
  window.removeEventListener('resize', onWindowResize)
  if (editorResizeObserver && editorPanelRef.value) {
    editorResizeObserver.unobserve(editorPanelRef.value)
    editorResizeObserver.disconnect()
    editorResizeObserver = null
  }
  if (iconHintHideTimeout !== null) {
    window.clearTimeout(iconHintHideTimeout)
    iconHintHideTimeout = null
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

const userRoles = computed(() => user.value?.roles ?? [])

const roleDisplay = computed(() => {
  const code = user.value?.roleType
  if (!code) return null
  const map: Record<string, { tag: string }> = {
    USER: { tag: '学员' },
    TEACHER: { tag: '教师' },
    SCHOOL: { tag: '校方' },
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

const handleSubmit = async () => {
  if (!isAuthed.value) {
    openLogin()
    return
  }

  const pid = problemDetail.value?.id
  if (!pid) {
    ElMessage.error('题目未加载完成，请稍后再试')
    return
  }

  submitting.value = true
  try {
    const res = await createSubmission({
      problemId: pid,
      language: activeLanguage.value,
      sourceCode: code.value,
    })
    const body: any = res.data
    const data = body && typeof body === 'object' && 'data' in body ? body.data : body
    const status = data?.status
    if (status === 'QUEUED') {
      ElMessage.success('提交成功，已加入评测队列（占位）')
    } else {
      ElMessage.success('提交成功')
    }
  } catch (e) {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
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
            <ProblemTimer />
            <button
              v-if="!isAuthed"
              type="button"
              class="solve-login-btn"
              @click="openLogin"
            >
              登录
            </button>
            <div
              v-else
              class="solve-user"
              @mouseenter="onUserEnter"
              @mouseleave="onUserLeave"
            >
              <UserAvatar
                :name="displayName"
                :size="28"
                :role-type="user?.roleType"
                :avatar="user?.avatar || null"
                class="solve-avatar"
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
                      v-if="userRoles.includes('STUDENT')"
                      to="/student"
                      class="user-menu__item"
                    >
                      学员中心
                    </RouterLink>
                    <RouterLink
                      v-if="userRoles.includes('TEACHER')"
                      to="/teacher"
                      class="user-menu__item"
                    >
                      教师后台
                    </RouterLink>
                    <RouterLink
                      v-if="userRoles.includes('SCHOOL')"
                      to="/school"
                      class="user-menu__item"
                    >
                      校方管理
                    </RouterLink>
                    <RouterLink
                      v-if="userRoles.includes('ADMIN')"
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
              <div class="icon-hint-wrapper" @mouseenter="onIconHintEnter('run')" @mouseleave="onIconHintLeave">
                <button
                  type="button"
                  class="btn-secondary btn-icon"
                  aria-label="调用测试用例运行"
                  @click="runCodeWithTestCases"
                >
                  <span class="btn-icon__glyph" aria-hidden="true">▶</span>
                </button>
                <transition name="fade">
                  <div v-if="iconHint === 'run'" class="icon-hint-panel">调用测试用例运行</div>
                </transition>
              </div>
              <div class="icon-hint-wrapper" @mouseenter="onIconHintEnter('reset')" @mouseleave="onIconHintLeave">
                <button
                  type="button"
                  class="btn-secondary btn-icon"
                  aria-label="还原到默认的代码模版"
                  @click="resetCodeToDefault"
                >
                  <span class="btn-icon__glyph" aria-hidden="true">↻</span>
                </button>
                <transition name="fade">
                  <div v-if="iconHint === 'reset'" class="icon-hint-panel">还原到默认的代码模版</div>
                </transition>
              </div>
              <button
                type="button"
                class="btn-primary"
                :disabled="submitting"
                @click="handleSubmit"
              >
                {{ submitting ? '提交中...' : '提交' }}
              </button>
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
              <div class="testcase-tabs">
                <button
                  v-for="(item, index) in testCases"
                  :key="item.id"
                  type="button"
                  class="testcase-tab"
                  :class="{
                    'testcase-tab--active': index === activeCaseIndex,
                  }"
                  @click="onSelectTestCase(index)"
                  @mouseenter="hoveredCaseIndex = index"
                  @mouseleave="hoveredCaseIndex = -1"
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
                    v-if="hoveredCaseIndex === index && testCases.length > 1"
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
                  v-if="testCases.length < 8"
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
                testCases.length &&
                activeTestCase &&
                activeCaseIndex >= 0 &&
                activeCaseIndex < testCases.length
              "
              class="testcase-body"
            >
              <div
                v-for="(input, inputIndex) in activeTestCase.inputs"
                :key="`${activeTestCase.id}-${input.label}-${inputIndex}`"
                class="testcase-row"
              >
                <span class="testcase-label">{{ input.label }}</span>
                <input
                  :value="input.value"
                  @input="updateTestCaseInput(activeCaseIndex, inputIndex, ($event.target as HTMLInputElement).value)"
                  class="testcase-input"
                  spellcheck="false"
                />
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

.solve-login-btn {
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  color: #4b5563;
  cursor: pointer;
}

.solve-login-btn:hover {
  background-color: #f9fafb;
}

.solve-avatar {
  cursor: pointer;
}

.solve-user {
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
  top: 36px;
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

.role-badge--student {
  background: linear-gradient(135deg, #818cf8, #6366f1);
}

.role-badge--teacher {
  background: linear-gradient(135deg, #34d399, #059669);
}

.role-badge--school {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
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
}

.btn-secondary,
.btn-primary {
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid transparent;
}

.btn-icon {
  width: 34px;
  padding: 5px 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  line-height: 1;
}

.btn-icon__glyph {
  display: inline-block;
  transform: translateY(-0.5px);
}

.icon-hint-wrapper {
  position: relative;
  display: inline-flex;
}

.icon-hint-panel {
  position: absolute;
  top: -29px;
  left: -47px;
  width: 120px;
  padding: 8px 10px;
  border-radius: 10px;
  background-color: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  z-index: 30;
  font-size: 12px;
  color: #374151;
  line-height: 1;
  text-align: center;
}

.editor-header-right .icon-hint-wrapper:nth-child(2) .icon-hint-panel {
  top: -29px;
  left: -61px;
  width: 150px;
}

.btn-secondary {
  background-color: #f3f4f6;
  color: #374151;
  border-color: #e5e7eb;
}

.btn-secondary:hover {
  background-color: #e5e7eb;
}

.btn-primary {
  background-color: #22c55e;
  color: #ffffff;
}

.btn-primary:hover {
  background-color: #16a34a;
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
  align-items: center;
  gap: 8px;
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

