<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import LoginDialog from '@/components/auth/LoginDialog.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { useAuth } from '@/hooks/useAuth'
import {
  getProblemDetailByNo,
  getProblemSampleTestCases,
  getProblemCodeDraft,
  runProblemCode,
  saveProblemCodeDraft,
  submitProblemCode,
  type ProblemCodeRunCaseResult,
  type ProblemCodeRunResult,
  type ProblemCodeSubmissionResult,
} from '@/api/problem'
import { renderMarkdown } from '@/utils/markdown'
import type { SupportedLanguage } from '@/constants/languageTemplates'
import { getProblemDefaultTestCases, getProblemTemplate } from '@/utils/problemPresets'

const route = useRoute()
const router = useRouter()
const { isAuthed, user, logout } = useAuth()

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
const codeCursorLine = ref(1)
const codeCursorColumn = ref(1)

const lineNumbers = computed(() =>
  Array.from({ length: Math.max(1, code.value.split('\n').length) }, (_, i) => i + 1),
)

const codeLineCount = computed(() => Math.max(1, code.value.split('\n').length))

const updateCodeCursorPosition = () => {
  const textarea = codeEditorRef.value
  if (!textarea) return
  const beforeCursor = textarea.value.slice(0, textarea.selectionStart)
  const lines = beforeCursor.split('\n')
  codeCursorLine.value = lines.length
  codeCursorColumn.value = (lines[lines.length - 1]?.length ?? 0) + 1
}

const syncCodeScroll = () => {
  if (!codeEditorRef.value || !lineNumbersRef.value) return
  lineNumbersRef.value.scrollTop = codeEditorRef.value.scrollTop
}

const resetCodeToDefault = async () => {
  try {
    await ElMessageBox.confirm(
      '重置后当前编辑区代码会恢复为默认模板，未保存的修改将被覆盖。确认重置吗？',
      '重置代码',
      {
        confirmButtonText: '重置',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  await setCodeWithoutDraftSave(resolveTemplate(activeLanguage.value, problemDetail.value))
  clearDraftSaveTimer()
  draftSyncStatus.value = 'idle'
  await nextTick(() => {
    if (codeEditorRef.value) codeEditorRef.value.scrollTop = 0
    if (lineNumbersRef.value) lineNumbersRef.value.scrollTop = 0
    updateCodeCursorPosition()
  })
}

const setCodeWithoutDraftSave = async (value: string) => {
  suppressDraftSave = true
  code.value = value
  await nextTick()
  suppressDraftSave = false
  resetCodeEditorHistory(value)
  updateCodeCursorPosition()
}

const clearDraftSaveTimer = () => {
  if (draftSaveTimer) {
    clearTimeout(draftSaveTimer)
    draftSaveTimer = null
  }
}

const CODE_INDENT = '    '
const CODE_HISTORY_LIMIT = 100

interface CodeEditorSnapshot {
  value: string
  selectionStart: number
  selectionEnd: number
}

const codeEditorHistory: CodeEditorSnapshot[] = []
let codeEditorHistoryIndex = -1

const saveCodeDraftNow = async (language: SupportedLanguage, sourceCode: string) => {
  if (!isAuthed.value || !problemDetail.value) return
  const seq = ++draftSaveSeq
  draftSyncStatus.value = 'saving'
  try {
    await saveProblemCodeDraft(Number(routeProblemNo.value), {
      language,
      sourceCode,
    })
    if (seq === draftSaveSeq) {
      draftSyncStatus.value = 'saved'
    }
  } catch {
    if (seq === draftSaveSeq) {
      draftSyncStatus.value = 'error'
    }
  }
}

const scheduleCodeDraftSave = (language: SupportedLanguage, sourceCode: string) => {
  if (!isAuthed.value || !problemDetail.value) return
  clearDraftSaveTimer()
  draftSaveTimer = setTimeout(() => {
    draftSaveTimer = null
    void saveCodeDraftNow(language, sourceCode)
  }, 800)
}

const saveCurrentCodeDraftNow = async () => {
  clearDraftSaveTimer()
  await saveCodeDraftNow(activeLanguage.value, code.value)
}

const isSameCodeEditorSnapshot = (a: CodeEditorSnapshot | undefined, b: CodeEditorSnapshot) =>
  !!a && a.value === b.value && a.selectionStart === b.selectionStart && a.selectionEnd === b.selectionEnd

const recordCodeEditorHistory = (snapshot: CodeEditorSnapshot) => {
  const current = codeEditorHistory[codeEditorHistoryIndex]
  if (isSameCodeEditorSnapshot(current, snapshot)) return

  if (codeEditorHistoryIndex < codeEditorHistory.length - 1) {
    codeEditorHistory.splice(codeEditorHistoryIndex + 1)
  }

  codeEditorHistory.push(snapshot)
  if (codeEditorHistory.length > CODE_HISTORY_LIMIT) {
    codeEditorHistory.shift()
  }
  codeEditorHistoryIndex = codeEditorHistory.length - 1
}

const resetCodeEditorHistory = (value: string) => {
  codeEditorHistory.splice(0, codeEditorHistory.length, {
    value,
    selectionStart: 0,
    selectionEnd: 0,
  })
  codeEditorHistoryIndex = 0
}

const applyCodeEditorValue = (
  textarea: HTMLTextAreaElement,
  nextValue: string,
  nextSelectionStart: number,
  nextSelectionEnd: number,
  recordHistory = true,
) => {
  code.value = nextValue
  textarea.value = nextValue
  if (recordHistory) {
    recordCodeEditorHistory({
      value: nextValue,
      selectionStart: nextSelectionStart,
      selectionEnd: nextSelectionEnd,
    })
  }
  nextTick(() => {
    textarea.setSelectionRange(nextSelectionStart, nextSelectionEnd)
    updateCodeCursorPosition()
  })
}

const restoreCodeEditorSnapshot = (textarea: HTMLTextAreaElement, snapshot: CodeEditorSnapshot) => {
  applyCodeEditorValue(textarea, snapshot.value, snapshot.selectionStart, snapshot.selectionEnd, false)
}

const syncCurrentCodeEditorSnapshot = (textarea: HTMLTextAreaElement) => {
  recordCodeEditorHistory({
    value: textarea.value,
    selectionStart: textarea.selectionStart,
    selectionEnd: textarea.selectionEnd,
  })
}

const undoCodeEditorChange = (textarea: HTMLTextAreaElement) => {
  syncCurrentCodeEditorSnapshot(textarea)
  if (codeEditorHistoryIndex <= 0) return
  codeEditorHistoryIndex -= 1
  const snapshot = codeEditorHistory[codeEditorHistoryIndex]
  if (snapshot) restoreCodeEditorSnapshot(textarea, snapshot)
}

const redoCodeEditorChange = (textarea: HTMLTextAreaElement) => {
  if (codeEditorHistoryIndex >= codeEditorHistory.length - 1) return
  codeEditorHistoryIndex += 1
  const snapshot = codeEditorHistory[codeEditorHistoryIndex]
  if (snapshot) restoreCodeEditorSnapshot(textarea, snapshot)
}

const handleCodeEditorInput = (event: Event) => {
  const textarea = event.currentTarget as HTMLTextAreaElement | null
  if (!textarea) return
  syncCurrentCodeEditorSnapshot(textarea)
}

const countCodeChar = (value: string, char: string) =>
  Array.from(value).filter((item) => item === char).length

const formatBraceCode = (sourceCode: string) => {
  let indentLevel = 0
  return sourceCode
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((rawLine) => {
      const line = rawLine.replace(/\t/g, CODE_INDENT).trim()
      if (!line) return ''

      const startsWithClosingBrace = line.startsWith('}')
      if (startsWithClosingBrace) {
        indentLevel = Math.max(0, indentLevel - 1)
      }

      const formattedLine = `${CODE_INDENT.repeat(indentLevel)}${line}`
      const openBraceCount = countCodeChar(line, '{')
      const closeBraceCount = countCodeChar(line, '}')
      indentLevel = Math.max(0, indentLevel + openBraceCount - closeBraceCount + (startsWithClosingBrace ? 1 : 0))
      return formattedLine
    })
    .join('\n')
}

const formatPythonCode = (sourceCode: string) => {
  let indentLevel = 0
  const dedentPattern = /^(elif|else|except|finally)\b/
  const outdentAfterPattern = /^(return|break|continue|pass|raise)\b/

  return sourceCode
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((rawLine) => {
      const line = rawLine.replace(/\t/g, CODE_INDENT).trim()
      if (!line) return ''

      if (dedentPattern.test(line)) {
        indentLevel = Math.max(0, indentLevel - 1)
      }

      const formattedLine = `${CODE_INDENT.repeat(indentLevel)}${line}`
      if (line.endsWith(':')) {
        indentLevel += 1
      } else if (outdentAfterPattern.test(line)) {
        indentLevel = Math.max(0, indentLevel - 1)
      }
      return formattedLine
    })
    .join('\n')
}

const formatCodeForLanguage = (sourceCode: string, language: SupportedLanguage) => {
  if (language === 'Python3') return formatPythonCode(sourceCode)
  return formatBraceCode(sourceCode)
}

const formatCodeEditor = (textarea: HTMLTextAreaElement) => {
  const { selectionStart, selectionEnd, value } = textarea
  syncCurrentCodeEditorSnapshot(textarea)

  const nextValue = formatCodeForLanguage(value, activeLanguage.value)
  const hadSelection = selectionStart !== selectionEnd
  const selectedWholeFile = selectionStart === 0 && selectionEnd === value.length
  const nextSelectionStart = selectedWholeFile ? 0 : Math.min(selectionStart, nextValue.length)
  const nextSelectionEnd = selectedWholeFile
    ? nextValue.length
    : Math.min(hadSelection ? selectionEnd : selectionStart, nextValue.length)

  applyCodeEditorValue(textarea, nextValue, nextSelectionStart, nextSelectionEnd)
}

const getSelectedLineBounds = (value: string, selectionStart: number, selectionEnd: number) => {
  const lineStart = value.lastIndexOf('\n', selectionStart - 1) + 1
  const effectiveEnd =
    selectionEnd > selectionStart && value[selectionEnd - 1] === '\n'
      ? selectionEnd - 1
      : selectionEnd
  const nextLineBreak = value.indexOf('\n', effectiveEnd)
  const lineEnd = nextLineBreak === -1 ? value.length : nextLineBreak
  return { lineStart, lineEnd }
}

const indentCodeSelection = (textarea: HTMLTextAreaElement) => {
  const { value, selectionStart, selectionEnd } = textarea

  if (selectionStart === selectionEnd) {
    const nextCursor = selectionStart + CODE_INDENT.length
    applyCodeEditorValue(
      textarea,
      `${value.slice(0, selectionStart)}${CODE_INDENT}${value.slice(selectionEnd)}`,
      nextCursor,
      nextCursor,
    )
    return
  }

  const { lineStart, lineEnd } = getSelectedLineBounds(value, selectionStart, selectionEnd)
  const selectedLines = value.slice(lineStart, lineEnd).split('\n')
  const nextBlock = selectedLines.map((line) => `${CODE_INDENT}${line}`).join('\n')
  const insertedChars = selectedLines.length * CODE_INDENT.length
  const nextSelectionStart =
    selectionStart === lineStart ? selectionStart : selectionStart + CODE_INDENT.length

  applyCodeEditorValue(
    textarea,
    `${value.slice(0, lineStart)}${nextBlock}${value.slice(lineEnd)}`,
    nextSelectionStart,
    selectionEnd + insertedChars,
  )
}

const unindentCodeSelection = (textarea: HTMLTextAreaElement) => {
  const { value, selectionStart, selectionEnd } = textarea
  const { lineStart, lineEnd } = getSelectedLineBounds(value, selectionStart, selectionEnd)
  const selectedLines = value.slice(lineStart, lineEnd).split('\n')
  const lineStarts: number[] = []
  const removedCounts: number[] = []
  let offset = lineStart

  const nextBlock = selectedLines
    .map((line) => {
      lineStarts.push(offset)
      offset += line.length + 1
      const leadingSpaces = line.match(/^ */)?.[0].length ?? 0
      const removeCount = Math.min(CODE_INDENT.length, leadingSpaces)
      removedCounts.push(removeCount)
      return line.slice(removeCount)
    })
    .join('\n')

  const removedBeforePosition = (position: number) =>
    removedCounts.reduce((total, count, index) => {
      const currentLineStart = lineStarts[index]
      if (currentLineStart == null) return total
      if (position <= currentLineStart) return total
      return total + Math.min(count, position - currentLineStart)
    }, 0)

  applyCodeEditorValue(
    textarea,
    `${value.slice(0, lineStart)}${nextBlock}${value.slice(lineEnd)}`,
    selectionStart - removedBeforePosition(selectionStart),
    selectionEnd - removedBeforePosition(selectionEnd),
  )
}

const getLineCommentToken = (language: SupportedLanguage) => (language === 'Python3' ? '#' : '//')

const toggleLineCommentSelection = (textarea: HTMLTextAreaElement) => {
  const { value, selectionStart, selectionEnd } = textarea
  const { lineStart, lineEnd } = getSelectedLineBounds(value, selectionStart, selectionEnd)
  const commentToken = getLineCommentToken(activeLanguage.value)
  const selectedLines = value.slice(lineStart, lineEnd).split('\n')
  const commentPattern = new RegExp(`^(\\s*)${commentToken.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')} ?`)
  const nonEmptyLines = selectedLines.filter((line) => line.trim())
  const shouldUncomment = nonEmptyLines.length > 0 && nonEmptyLines.every((line) => commentPattern.test(line))

  const nextBlock = selectedLines
    .map((line) => {
      if (!line.trim()) return line
      if (shouldUncomment) {
        return line.replace(commentPattern, '$1')
      }
      const leadingWhitespace = line.match(/^\s*/)?.[0] ?? ''
      return `${leadingWhitespace}${commentToken} ${line.slice(leadingWhitespace.length)}`
    })
    .join('\n')
  const nextValue = `${value.slice(0, lineStart)}${nextBlock}${value.slice(lineEnd)}`

  applyCodeEditorValue(
    textarea,
    nextValue,
    selectionStart === selectionEnd ? Math.min(selectionStart, nextValue.length) : lineStart,
    selectionStart === selectionEnd ? Math.min(selectionStart, nextValue.length) : lineStart + nextBlock.length,
  )
}

const duplicateCodeSelection = (textarea: HTMLTextAreaElement) => {
  const { value, selectionStart, selectionEnd } = textarea

  if (selectionStart !== selectionEnd) {
    const selectedText = value.slice(selectionStart, selectionEnd)
    applyCodeEditorValue(
      textarea,
      `${value.slice(0, selectionEnd)}${selectedText}${value.slice(selectionEnd)}`,
      selectionEnd,
      selectionEnd + selectedText.length,
    )
    return
  }

  const { lineStart, lineEnd } = getSelectedLineBounds(value, selectionStart, selectionEnd)
  const lineText = value.slice(lineStart, lineEnd)
  const duplicatedText = `\n${lineText}`
  const nextCursor = selectionStart + duplicatedText.length

  applyCodeEditorValue(
    textarea,
    `${value.slice(0, lineEnd)}${duplicatedText}${value.slice(lineEnd)}`,
    nextCursor,
    nextCursor,
  )
}

const deleteCurrentCodeLine = (textarea: HTMLTextAreaElement) => {
  const { value, selectionStart, selectionEnd } = textarea
  const { lineStart, lineEnd } = getSelectedLineBounds(value, selectionStart, selectionEnd)
  let deleteStart = lineStart
  let deleteEnd = lineEnd

  if (value[deleteEnd] === '\n') {
    deleteEnd += 1
  } else if (deleteStart > 0) {
    deleteStart -= 1
  }

  const nextValue = `${value.slice(0, deleteStart)}${value.slice(deleteEnd)}`
  const nextCursor = Math.min(deleteStart, nextValue.length)

  applyCodeEditorValue(textarea, nextValue, nextCursor, nextCursor)
}

const withCodeEditor = (action: (textarea: HTMLTextAreaElement) => void) => {
  const textarea = codeEditorRef.value
  if (!textarea) return
  action(textarea)
  textarea.focus()
}

const handleFormatCodeClick = () => {
  withCodeEditor(formatCodeEditor)
}

const handleToggleCommentClick = () => {
  withCodeEditor(toggleLineCommentSelection)
}

const openShortcutTips = () => {
  shortcutTipsVisible.value = true
}

const handleCodeEditorKeydown = (event: KeyboardEvent) => {
  const textarea = event.currentTarget as HTMLTextAreaElement | null
  const isModifierShortcut = event.ctrlKey || event.metaKey

  if (event.key === 'Tab' && textarea) {
    event.preventDefault()
    if (event.shiftKey) {
      unindentCodeSelection(textarea)
      return
    }
    indentCodeSelection(textarea)
    return
  }

  if (!isModifierShortcut || !textarea) return

  const key = event.key.toLowerCase()

  if (event.altKey && key === 'l') {
    event.preventDefault()
    formatCodeEditor(textarea)
    return
  }

  if (key === 's') {
    event.preventDefault()
    void saveCurrentCodeDraftNow()
    return
  }

  if (key === '/') {
    event.preventDefault()
    toggleLineCommentSelection(textarea)
    return
  }

  if (key === 'd') {
    event.preventDefault()
    duplicateCodeSelection(textarea)
    return
  }

  if (event.shiftKey && key === 'k') {
    event.preventDefault()
    deleteCurrentCodeLine(textarea)
    return
  }

  if (key === 'z') {
    event.preventDefault()
    if (event.shiftKey) {
      redoCodeEditorChange(textarea)
      return
    }
    undoCodeEditorChange(textarea)
    return
  }

  if (key === 'y' || key === 'r') {
    event.preventDefault()
    redoCodeEditorChange(textarea)
  }
}

const loadCodeDraft = async (language: SupportedLanguage) => {
  if (!isAuthed.value) return false
  draftSyncStatus.value = 'loading'
  try {
    const result = await getProblemCodeDraft(Number(routeProblemNo.value), language)
    if (result.data?.sourceCode != null) {
      await setCodeWithoutDraftSave(result.data.sourceCode)
      draftSyncStatus.value = 'saved'
      return true
    }
    draftSyncStatus.value = 'idle'
    return false
  } catch {
    draftSyncStatus.value = 'error'
    return false
  }
}

const applyLanguageCode = async (language: SupportedLanguage) => {
  await setCodeWithoutDraftSave(resolveTemplate(language, problemDetail.value))
  await loadCodeDraft(language)
  await nextTick(() => {
    if (codeEditorRef.value) codeEditorRef.value.scrollTop = 0
    if (lineNumbersRef.value) lineNumbersRef.value.scrollTop = 0
  })
}

const isRunning = ref(false)
const isSubmitting = ref(false)
const runDialogVisible = ref(false)
const runPending = ref(false)
const runResult = ref<ProblemCodeRunResult | null>(null)
const runError = ref('')
const submitDialogVisible = ref(false)
const submitPending = ref(false)
const submitResult = ref<ProblemCodeSubmissionResult | null>(null)
const submitError = ref('')
const draftSyncStatus = ref<'idle' | 'loading' | 'saving' | 'saved' | 'error'>('idle')
const isTiming = ref(false)
const elapsedSeconds = ref(0)
let timerHandle: ReturnType<typeof setInterval> | null = null
let draftSaveTimer: ReturnType<typeof setTimeout> | null = null
let suppressDraftSave = false
let draftSaveSeq = 0

const draftSyncText = computed(() => {
  const map = {
    idle: '未同步',
    loading: '读取草稿',
    saving: '同步中',
    saved: '已同步',
    error: '同步失败',
  }
  return map[draftSyncStatus.value]
})

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

  testCases.value.forEach((item) => {
    item.status = 'default'
  })

  const cases = testCases.value.map((item) => ({
    inputText: item.inputText,
    expectedOutput: item.outputText,
  }))
  if (!cases.length) {
    ElMessage.warning('请先添加测试用例')
    return
  }

  try {
    isRunning.value = true
    runPending.value = true
    runError.value = ''
    runResult.value = null
    runDialogVisible.value = true
    const result = await runProblemCode({
      language: activeLanguage.value,
      sourceCode: code.value,
      timeLimitMs: problemDetail.value?.timeLimitMs,
      memoryLimitKb: problemDetail.value?.memoryLimitKb,
      cases,
    })
    runResult.value = result.data
    result.data.caseResults.forEach((caseResult) => {
      const target = testCases.value[caseResult.caseIndex]
      if (!target) return
      target.status = caseResult.status === 'AC' ? 'success' : 'failed'
    })
    const allPassed = result.data.caseResults.every((item) => item.status === 'AC')
    if (allPassed) {
      ElMessage.success('运行通过')
    } else {
      ElMessage.warning('运行完成，存在未通过用例')
    }
  } catch (e) {
    runError.value = e instanceof Error ? e.message : '运行失败'
  } finally {
    runPending.value = false
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
    submitPending.value = true
    submitError.value = ''
    submitResult.value = null
    submitDialogVisible.value = true
    const result = await submitProblemCode(Number(routeProblemNo.value), {
      language: activeLanguage.value,
      sourceCode: code.value,
    })
    submitResult.value = result.data
    ElMessage.success(result.data.message || '代码已提交')
    await loadProblemDetail()
  } catch (e) {
    submitError.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    submitPending.value = false
    isSubmitting.value = false
  }
}

const closeSubmitDialog = () => {
  if (submitPending.value) return
  submitDialogVisible.value = false
}

const closeRunDialog = () => {
  if (runPending.value) return
  runDialogVisible.value = false
}

const statusText = (status?: string | null) => {
  const map: Record<string, string> = {
    AC: '通过',
    WA: '答案错误',
    TLE: '运行超时',
    MLE: '内存超限',
    RE: '运行错误',
    CE: '编译失败',
    SYSTEM_ERROR: '系统错误',
    RUNNING: '判题中',
    QUEUED: '排队中',
    FINISHED: '运行完成',
  }
  return status ? map[status] || status : '--'
}

const caseTitle = (item: ProblemCodeRunCaseResult, index: number) => {
  const prefix = item.caseType === 'HIDDEN' ? '隐藏用例' : item.caseType === 'SAMPLE' ? '公开样例' : '测试用例'
  return `${prefix} ${index + 1}`
}

const caseDisplayText = (value?: string | null) => {
  if (value == null || value === '') return '--'
  return value
}

const copyErrorText = async (value?: string | null) => {
  const text = caseDisplayText(value)
  if (!text || text === '--') return
  try {
    await navigator.clipboard?.writeText(text)
    ElMessage.success('错误信息已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择错误信息')
  }
}

const passedCaseCount = computed(() =>
  submitResult.value?.caseResults?.filter((item) => item.status === 'AC').length ?? 0,
)

const submitTotalCaseCount = computed(() =>
  submitResult.value?.totalCaseCount ?? submitResult.value?.caseResults?.length ?? 0,
)

const submitFailedCase = computed(() =>
  submitResult.value?.caseResults?.find((item) => item.status !== 'AC') ?? null,
)

const submitTimeBuckets = computed(() =>
  submitResult.value?.rankStats?.timeBuckets ?? [],
)

const maxBucketCount = (items: Array<{ count?: number | null }>) =>
  Math.max(1, ...items.map((item) => item.count ?? 0))

const bucketPercent = (count: number | null | undefined, items: Array<{ count?: number | null }>) =>
  `${Math.max(0, ((count ?? 0) / maxBucketCount(items)) * 100)}%`

const runPassedCaseCount = computed(() =>
  runResult.value?.caseResults?.filter((item) => item.status === 'AC').length ?? 0,
)

const runTotalTimeMs = computed(() => {
  const caseResults = runResult.value?.caseResults ?? []
  if (!caseResults.length) return null
  return Math.max(...caseResults.map((item) => item.timeMs ?? 0))
})

watch(
  activeLanguage,
  async (nextLang, prevLang) => {
    if (!prevLang || nextLang === prevLang) return

    const prevTemplate = resolveTemplate(prevLang, problemDetail.value).trim()
    const currentCode = code.value.trim()

    const isUnsyncedEditedCode = currentCode !== prevTemplate && draftSyncStatus.value !== 'saved'

    if (!isUnsyncedEditedCode) {
      await applyLanguageCode(nextLang)
      return
    }

    try {
      await ElMessageBox.confirm(
        '切换语言将加载该语言的模板或服务器草稿。确认切换吗？',
        '切换语言',
        {
          confirmButtonText: '切换',
          cancelButtonText: '取消',
          type: 'warning',
        },
      )
      await applyLanguageCode(nextLang)
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
    await applyLanguageCode(activeLanguage.value)
    testCases.value = (
      sampleCaseRes.data?.length ? sampleCaseRes.data : getProblemDefaultTestCases(problemDetail.value)
    ).map(toTestCase)
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

const activeCaseIndex = ref(0)
const hoveredCaseIndex = ref(-1)

const activeTestCase = computed(() => {
  if (!testCases.value.length) return null
  const index = activeCaseIndex.value >= 0 && activeCaseIndex.value < testCases.value.length ? activeCaseIndex.value : 0
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
  if (!testCases.value.length) return
  if (caseIndex < 0 || caseIndex >= testCases.value.length) return
  const target = testCases.value[caseIndex]
  if (!target) return
  target.inputText = value
}

const updateTestCaseOutput = (caseIndex: number, value: string) => {
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
const editorTopMinHeight = 220
const editorBottomMinHeight = 180
const splitRatio = ref(0.6) // 上方代码区域所占比例
const editorStorageKey = computed(() => `OJPT.solve.editorSplitRatio`)
const editorPanelHeight = ref(0) // 右侧面板当前高度，用于按比例计算上下区域

const shortcutTipsStorageKey = 'OJPT.solve.shortcutTipsHidden'
const shortcutTipsVisible = ref(false)
const shortcutTipsDontShowAgain = ref(false)
const shortcutTipItems = [
  { keys: 'Ctrl/Cmd + S', action: '立即保存草稿' },
  { keys: 'Tab / Shift + Tab', action: '增加或减少缩进' },
  { keys: 'Ctrl/Cmd + Z', action: '撤销编辑' },
  { keys: 'Ctrl/Cmd + R / Y / Shift + Z', action: '重做编辑' },
  { keys: 'Ctrl + Alt + L / Cmd + Option + L', action: '整理代码格式' },
  { keys: 'Ctrl/Cmd + /', action: '切换行注释' },
  { keys: 'Ctrl/Cmd + D', action: '复制当前行或选区' },
  { keys: 'Ctrl/Cmd + Shift + K', action: '删除当前行或选中行' },
]

const showShortcutTipsIfNeeded = () => {
  shortcutTipsVisible.value = localStorage.getItem(shortcutTipsStorageKey) !== 'true'
}

const closeShortcutTips = () => {
  if (shortcutTipsDontShowAgain.value) {
    localStorage.setItem(shortcutTipsStorageKey, 'true')
  }
  shortcutTipsVisible.value = false
}

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
    const minRatio = Math.min(1, editorTopMinHeight / availableHeight)
    const maxRatio = Math.max(0, (availableHeight - editorBottomMinHeight) / availableHeight)
    const lower = Math.min(minRatio, maxRatio)
    const upper = Math.max(minRatio, maxRatio)
    splitRatio.value = Math.min(upper, Math.max(lower, nextRatio))
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
  showShortcutTipsIfNeeded()
})

onBeforeUnmount(() => {
  stopResizing()
  stopEditorResizing()
  stopTimer()
  clearDraftSaveTimer()
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

watch(code, (nextCode) => {
  if (suppressDraftSave || loadingProblem.value || !problemDetail.value) return
  scheduleCodeDraftSave(activeLanguage.value, nextCode)
})

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
              <span
                v-if="isAuthed"
                class="draft-sync"
                :class="`draft-sync--${draftSyncStatus}`"
              >
                {{ draftSyncText }}
              </span>
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
                  aria-label="格式化代码"
                  title="格式化代码 (Ctrl+Alt+L / Cmd+Option+L)"
                  data-testid="format-code-button"
                  @click="handleFormatCodeClick"
                >
                  <span class="btn-icon-text" aria-hidden="true">{{ '{ }' }}</span>
                </button>
              </div>
              <div class="icon-hint-wrapper">
                <button
                  type="button"
                  class="btn-icon-refresh"
                  aria-label="切换行注释"
                  title="切换行注释 (Ctrl/Cmd+/)"
                  data-testid="comment-code-button"
                  @click="handleToggleCommentClick"
                >
                  <span class="btn-icon-text" aria-hidden="true">//</span>
                </button>
              </div>
              <div class="icon-hint-wrapper">
                <button
                  type="button"
                  class="btn-icon-refresh"
                  aria-label="查看快捷键"
                  title="查看快捷键"
                  data-testid="shortcut-help-button"
                  @click="openShortcutTips"
                >
                  <span class="btn-icon-text" aria-hidden="true">?</span>
                </button>
              </div>
              <div class="icon-hint-wrapper">
                <button
                  type="button"
                  class="btn-icon-refresh"
                  aria-label="重置为默认代码模板"
                  title="重置为默认代码模板"
                  data-testid="reset-code-button"
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
                @input="handleCodeEditorInput"
                @keydown="handleCodeEditorKeydown"
                @click="updateCodeCursorPosition"
                @keyup="updateCodeCursorPosition"
                @select="updateCodeCursorPosition"
                @scroll="syncCodeScroll"
              />
            </div>
            <footer class="code-editor-status" data-testid="code-editor-status-bar">
              <span>{{ activeLanguage }}</span>
              <span>行 {{ codeCursorLine }}，列 {{ codeCursorColumn }}</span>
              <span>{{ draftSyncText }}</span>
              <span>{{ codeLineCount }} 行</span>
            </footer>
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
              <div class="testcase-row testcase-row--stacked">
                <span class="testcase-label">输入</span>
                <textarea
                  :value="activeTestCase.inputText"
                  @input="updateTestCaseInput(activeCaseIndex, ($event.target as HTMLTextAreaElement).value)"
                  class="testcase-textarea"
                  spellcheck="false"
                  rows="4"
                />
              </div>
              <div class="testcase-row testcase-row--stacked">
                <span class="testcase-label">期望输出</span>
                <textarea
                  :value="activeTestCase.outputText"
                  @input="updateTestCaseOutput(activeCaseIndex, ($event.target as HTMLTextAreaElement).value)"
                  class="testcase-textarea"
                  spellcheck="false"
                  rows="2"
                />
              </div>
              <div v-if="activeTestCase.explanation" class="testcase-explanation">
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
    <div v-if="runDialogVisible" class="submit-dialog-mask" @click.self="closeRunDialog">
      <section
        class="submit-dialog"
        role="dialog"
        aria-modal="true"
        aria-label="运行详情"
        data-testid="run-result-dialog"
      >
        <header class="submit-dialog__header">
          <div>
            <h2 class="submit-dialog__title">运行详情</h2>
            <p class="submit-dialog__subtitle">
              {{ problemDetail ? `P${String(problemDetail.problemNo).padStart(4, '0')} · ${problemDetail.title}` : '当前题目' }}
            </p>
          </div>
          <button
            type="button"
            class="submit-dialog__close"
            :disabled="runPending"
            aria-label="关闭运行详情"
            @click="closeRunDialog"
          >
            ×
          </button>
        </header>

        <div v-if="runPending" class="submit-progress" data-testid="run-result-pending">
          <span class="submit-progress__spinner" aria-hidden="true" />
          <div>
            <strong>运行中</strong>
            <p>正在按顺序运行测试用例，遇到首个未通过用例会停止。</p>
          </div>
        </div>

        <div v-else-if="runError" class="submit-alert submit-alert--error submit-alert--with-action">
          <span>{{ runError }}</span>
          <button type="button" class="copy-error-btn" @click="copyErrorText(runError)">复制错误</button>
        </div>

        <template v-else-if="runResult">
          <div class="submit-summary" :class="`submit-summary--${runResult.status.toLowerCase()}`">
            <div>
              <span class="submit-summary__label">状态</span>
              <strong>{{ statusText(runResult.status) }}</strong>
            </div>
            <div>
              <span class="submit-summary__label">通过用例</span>
              <strong>{{ runPassedCaseCount }} / {{ runResult.caseResults?.length ?? 0 }}</strong>
            </div>
            <div>
              <span class="submit-summary__label">已运行用例</span>
              <strong>{{ runResult.caseResults?.length ?? 0 }}</strong>
            </div>
            <div>
              <span class="submit-summary__label">运行耗时</span>
              <strong>{{ runTotalTimeMs ?? '--' }} ms</strong>
            </div>
          </div>

          <div
            class="submit-alert"
            :class="runResult.caseResults?.every((item) => item.status === 'AC') ? 'submit-alert--success' : 'submit-alert--error'"
          >
            {{
              runResult.caseResults?.every((item) => item.status === 'AC')
                ? '测试通过。'
                : '测试未通过，已停止后续用例。'
            }}
          </div>

          <div class="submit-case-list">
            <article
              v-for="(item, index) in runResult.caseResults"
              :key="`${item.caseType}-${item.caseIndex}`"
              class="submit-case"
              :class="`submit-case--${item.status.toLowerCase()}`"
              data-testid="run-case-result"
            >
              <header class="submit-case__header">
                <strong>{{ caseTitle(item, index) }}</strong>
                <span>{{ statusText(item.status) }} · {{ item.timeMs ?? '--' }} ms</span>
              </header>
              <div class="submit-case__grid">
                <div>
                  <span>输入</span>
                  <pre>{{ caseDisplayText(item.inputText) }}</pre>
                </div>
                <div>
                  <span>期望输出</span>
                  <pre>{{ caseDisplayText(item.expectedOutput) }}</pre>
                </div>
                <div>
                  <span>实际输出</span>
                  <pre>{{ caseDisplayText(item.actualOutput) }}</pre>
                </div>
                <div v-if="item.errorOutput || item.message">
                  <span>错误信息</span>
                  <button type="button" class="copy-error-btn copy-error-btn--inline" @click="copyErrorText(item.errorOutput || item.message)">
                    复制错误
                  </button>
                  <pre>{{ caseDisplayText(item.errorOutput || item.message) }}</pre>
                </div>
              </div>
            </article>
          </div>
        </template>
      </section>
    </div>

    <div v-if="submitDialogVisible" class="submit-dialog-mask" @click.self="closeSubmitDialog">
      <section class="submit-dialog" role="dialog" aria-modal="true" aria-label="提交详情">
        <header class="submit-dialog__header">
          <div>
            <h2 class="submit-dialog__title">提交详情</h2>
            <p class="submit-dialog__subtitle">
              {{ problemDetail ? `P${String(problemDetail.problemNo).padStart(4, '0')} · ${problemDetail.title}` : '当前题目' }}
            </p>
          </div>
          <button
            type="button"
            class="submit-dialog__close"
            :disabled="submitPending"
            aria-label="关闭提交详情"
            @click="closeSubmitDialog"
          >
            ×
          </button>
        </header>

        <div v-if="submitPending" class="submit-progress">
          <span class="submit-progress__spinner" aria-hidden="true" />
          <div>
            <strong>判题中</strong>
            <p>正在运行公开样例和隐藏用例。</p>
          </div>
        </div>

        <div v-else-if="submitError" class="submit-alert submit-alert--error submit-alert--with-action">
          <span>{{ submitError }}</span>
          <button type="button" class="copy-error-btn" @click="copyErrorText(submitError)">复制错误</button>
        </div>

        <template v-else-if="submitResult">
          <div class="submit-detail-layout">
            <section class="submit-info-card">
              <div class="submit-summary submit-summary--compact" :class="`submit-summary--${submitResult.status.toLowerCase()}`">
                <div>
                  <span class="submit-summary__label">状态</span>
                  <strong>{{ statusText(submitResult.status) }}</strong>
                </div>
                <div>
                  <span class="submit-summary__label">通过用例</span>
                  <strong>{{ passedCaseCount }} / {{ submitTotalCaseCount }}</strong>
                </div>
                <div>
                  <span class="submit-summary__label">题目耗时</span>
                  <strong>{{ submitResult.timeMs ?? '--' }} ms</strong>
                </div>
              </div>

              <div v-if="submitResult.status === 'AC'" class="submit-alert submit-alert--success">
                已通过本题，按单个最慢用例耗时进入速度排名。
              </div>
              <div v-else class="submit-alert submit-alert--error">
                {{ submitResult.message || statusText(submitResult.status) }}
              </div>

              <article
                v-if="submitFailedCase"
                class="submit-case"
                :class="`submit-case--${submitFailedCase.status.toLowerCase()}`"
                data-testid="submit-case-result"
              >
                <header class="submit-case__header">
                  <strong>{{ caseTitle(submitFailedCase, submitFailedCase.caseIndex) }}</strong>
                  <span>{{ statusText(submitFailedCase.status) }} · {{ submitFailedCase.timeMs ?? '--' }} ms</span>
                </header>
                <div class="submit-case__grid">
                  <div>
                    <span>输入</span>
                    <pre>{{ caseDisplayText(submitFailedCase.inputText) }}</pre>
                  </div>
                  <div>
                    <span>期望输出</span>
                    <pre>{{ caseDisplayText(submitFailedCase.expectedOutput) }}</pre>
                  </div>
                  <div>
                    <span>实际输出</span>
                    <pre>{{ caseDisplayText(submitFailedCase.actualOutput) }}</pre>
                  </div>
                  <div v-if="submitFailedCase.errorOutput || submitFailedCase.message">
                    <span>错误信息</span>
                    <button
                      type="button"
                      class="copy-error-btn copy-error-btn--inline"
                      @click="copyErrorText(submitFailedCase.errorOutput || submitFailedCase.message)"
                    >
                      复制错误
                    </button>
                    <pre>{{ caseDisplayText(submitFailedCase.errorOutput || submitFailedCase.message) }}</pre>
                  </div>
                </div>
              </article>
            </section>

            <aside class="submit-rank-card">
              <div class="submit-rank-head">
                <div>
                  <span class="submit-summary__label">速度排名</span>
                  <strong>{{ submitResult.rank ? `第 ${submitResult.rank} 名` : '--' }}</strong>
                </div>
                <div>
                  <span class="submit-summary__label">AC 提交数</span>
                  <strong>{{ submitResult.rankStats?.acceptedCount ?? '--' }}</strong>
                </div>
              </div>

              <section class="rank-chart">
                <h3>耗时分布</h3>
                <div v-if="submitTimeBuckets.length" class="rank-column-chart">
                  <div
                    v-for="bucket in submitTimeBuckets"
                    :key="`time-${bucket.label}`"
                    class="rank-column"
                  >
                    <strong>{{ bucket.count }}</strong>
                    <div class="rank-column-track">
                      <i
                        :style="{ height: bucketPercent(bucket.count, submitTimeBuckets) }"
                        data-testid="time-bucket-bar"
                      />
                    </div>
                    <span>{{ bucket.label }}</span>
                  </div>
                </div>
                <p v-else class="rank-chart-empty">暂无耗时分布数据</p>
              </section>
            </aside>
          </div>
        </template>
      </section>
    </div>

    <div v-if="shortcutTipsVisible" class="submit-dialog-mask" @click.self="closeShortcutTips">
      <section
        class="shortcut-tips-dialog"
        role="dialog"
        aria-modal="true"
        aria-label="代码编辑器快捷键"
        data-testid="shortcut-tips-dialog"
      >
        <header class="shortcut-tips-dialog__header">
          <div>
            <h2 class="shortcut-tips-dialog__title">代码编辑器快捷键</h2>
            <p class="shortcut-tips-dialog__subtitle">这些快捷键只在右侧代码编辑区生效。</p>
          </div>
          <button
            type="button"
            class="submit-dialog__close"
            aria-label="关闭快捷键提示"
            @click="closeShortcutTips"
          >
            ×
          </button>
        </header>

        <div class="shortcut-tips-list">
          <div
            v-for="item in shortcutTipItems"
            :key="item.keys"
            class="shortcut-tip-item"
          >
            <kbd>{{ item.keys }}</kbd>
            <span>{{ item.action }}</span>
          </div>
        </div>

        <footer class="shortcut-tips-dialog__footer">
          <label class="shortcut-tips-checkbox">
            <input
              v-model="shortcutTipsDontShowAgain"
              type="checkbox"
              data-testid="shortcut-tips-dont-show"
            >
            <span>以后不再提示</span>
          </label>
          <button
            type="button"
            class="btn-primary"
            data-testid="shortcut-tips-close"
            @click="closeShortcutTips"
          >
            我知道了
          </button>
        </footer>
      </section>
    </div>

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

.submit-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(17, 24, 39, 0.42);
}

.submit-dialog {
  width: min(920px, 100%);
  max-height: min(760px, calc(100vh - 48px));
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.24);
}

.submit-dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid #e5e7eb;
}

.submit-dialog__title {
  margin: 0;
  font-size: 18px;
  line-height: 1.3;
  color: #111827;
}

.submit-dialog__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.submit-dialog__close {
  width: 32px;
  height: 32px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #ffffff;
  color: #374151;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.submit-dialog__close:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.shortcut-tips-dialog {
  width: min(560px, 100%);
  max-height: min(620px, calc(100vh - 48px));
  overflow: hidden;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.24);
  display: flex;
  flex-direction: column;
}

.shortcut-tips-dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid #e5e7eb;
}

.shortcut-tips-dialog__title {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.shortcut-tips-dialog__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.shortcut-tips-list {
  display: grid;
  gap: 8px;
  padding: 16px 20px;
  overflow-y: auto;
}

.shortcut-tip-item {
  display: grid;
  grid-template-columns: minmax(190px, 0.95fr) minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 9px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.shortcut-tip-item kbd {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-bottom-width: 2px;
  border-radius: 6px;
  background: #ffffff;
  color: #111827;
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.3;
  white-space: normal;
}

.shortcut-tip-item span {
  min-width: 0;
  color: #374151;
  font-size: 13px;
}

.shortcut-tips-dialog__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px 18px;
  border-top: 1px solid #e5e7eb;
}

.shortcut-tips-checkbox {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #4b5563;
  font-size: 13px;
  cursor: pointer;
}

.shortcut-tips-checkbox input {
  width: 14px;
  height: 14px;
  accent-color: #16a34a;
}

.submit-progress,
.submit-alert,
.submit-summary,
.submit-case-list {
  margin: 16px 20px 0;
}

.submit-progress {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1e3a8a;
}

.submit-progress p {
  margin: 4px 0 0;
  color: #3b4f7a;
}

.submit-progress__spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #bfdbfe;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: submit-spin 0.8s linear infinite;
}

.submit-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.submit-summary > div {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.submit-summary__label {
  display: block;
  margin-bottom: 5px;
  font-size: 12px;
  color: #6b7280;
}

.submit-summary strong {
  font-size: 17px;
  color: #111827;
}

.submit-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: 16px;
  margin: 16px 20px 20px;
  min-height: 0;
  overflow: auto;
}

.submit-info-card,
.submit-rank-card {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
}

.submit-info-card {
  padding: 14px;
}

.submit-rank-card {
  padding: 14px;
  background: #f9fafb;
}

.submit-info-card .submit-summary,
.submit-info-card .submit-alert,
.submit-info-card .submit-case,
.submit-rank-card .rank-chart {
  margin: 0;
}

.submit-info-card .submit-summary {
  position: sticky;
  top: 0;
  z-index: 1;
}

.submit-info-card .submit-alert,
.submit-info-card .submit-case {
  margin-top: 12px;
}

.submit-summary--compact {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.submit-rank-head {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.submit-rank-head > div {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.submit-rank-head strong {
  font-size: 17px;
  color: #111827;
}

.rank-chart {
  margin-top: 14px;
}

.rank-chart h3 {
  margin: 0 0 10px;
  font-size: 13px;
  color: #111827;
}

.rank-column-chart {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(42px, 1fr));
  align-items: end;
  gap: 8px;
  min-height: 190px;
  padding: 8px 4px 0;
}

.rank-column {
  min-width: 0;
  display: grid;
  grid-template-rows: 18px 132px auto;
  align-items: end;
  justify-items: center;
  gap: 6px;
}

.rank-column strong {
  color: #111827;
  font-size: 12px;
  line-height: 1;
}

.rank-column-track {
  width: 100%;
  max-width: 34px;
  height: 132px;
  display: flex;
  align-items: flex-end;
  border-radius: 8px 8px 4px 4px;
  background: #e5e7eb;
  overflow: hidden;
}

.rank-column-track i {
  display: block;
  width: 100%;
  min-height: 3px;
  border-radius: 8px 8px 0 0;
  background: #2563eb;
}

.rank-column span {
  max-width: 58px;
  min-height: 28px;
  color: #4b5563;
  font-size: 11px;
  line-height: 1.25;
  text-align: center;
  word-break: break-word;
}

.rank-chart-empty {
  margin: 0;
  padding: 12px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  background: #ffffff;
  color: #6b7280;
  font-size: 12px;
}

.submit-alert {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.submit-alert--success {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}

.submit-alert--error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
}

.submit-case-list {
  padding-bottom: 18px;
  overflow: auto;
}

.submit-case {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
}

.submit-case + .submit-case {
  margin-top: 12px;
}

.submit-case__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  color: #111827;
}

.submit-case__header span {
  color: #4b5563;
  font-size: 13px;
}

.submit-case--ac .submit-case__header {
  background: #f0fdf4;
}

.submit-case--wa .submit-case__header,
.submit-case--re .submit-case__header,
.submit-case--tle .submit-case__header,
.submit-case--ce .submit-case__header,
.submit-case--system_error .submit-case__header {
  background: #fef2f2;
}

.submit-case__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 12px;
}

.submit-case__grid span {
  display: block;
  margin-bottom: 5px;
  font-size: 12px;
  color: #6b7280;
}

.submit-case__grid pre {
  min-height: 42px;
  max-height: 150px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 9px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
  color: #111827;
  font-family: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.submit-alert--with-action {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.copy-error-btn {
  flex: 0 0 auto;
  border: 1px solid #fecaca;
  border-radius: 999px;
  background: #ffffff;
  color: #991b1b;
  padding: 3px 9px;
  font-size: 12px;
  cursor: pointer;
}

.copy-error-btn:hover {
  background: #fff1f2;
}

.copy-error-btn--inline {
  margin: 0 0 5px 8px;
  padding: 2px 8px;
}

@keyframes submit-spin {
  to {
    transform: rotate(360deg);
  }
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

.draft-sync {
  display: inline-flex;
  align-items: center;
  min-width: 54px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  color: #6b7280;
  font-size: 11px;
  white-space: nowrap;
}

.draft-sync--saving,
.draft-sync--loading {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.draft-sync--saved {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.draft-sync--error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
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

.btn-icon-text {
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.editor-body {
  flex: 1;
  padding: 8px 12px;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 6px;
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
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  overflow: hidden;
  box-sizing: border-box;
}

.code-editor-status {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  min-height: 22px;
  padding: 0 2px;
  color: #6b7280;
  font-size: 11px;
  white-space: nowrap;
}

.code-editor-status span + span {
  padding-left: 12px;
  border-left: 1px solid #e5e7eb;
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
  flex: 1;
  height: 100%;
  min-height: 0;
  overflow: hidden;
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
  display: grid;
  grid-template-rows: minmax(0, 1fr) minmax(0, 0.75fr) auto;
  gap: 6px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.testcase-row {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr);
  align-items: start;
  gap: 8px;
  min-height: 0;
}

.testcase-row--stacked {
  grid-template-columns: 60px minmax(0, 1fr);
  min-height: 0;
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
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  outline: none;
  resize: none;
  overflow: auto;
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
