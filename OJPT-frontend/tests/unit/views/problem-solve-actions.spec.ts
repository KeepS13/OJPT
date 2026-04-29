import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, nextTick } from 'vue'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import ElementPlus from 'element-plus'

const {
  getProblemDetailByNoMock,
  getProblemSampleTestCasesMock,
  getProblemCodeDraftMock,
  runProblemCodeMock,
  saveProblemCodeDraftMock,
  submitProblemCodeMock,
  renderMarkdownMock,
  getProblemDefaultTestCasesMock,
  getProblemTemplateMock,
  pushMock,
  successMessageMock,
  warningMessageMock,
  confirmMessageBoxMock,
} = vi.hoisted(() => ({
  getProblemDetailByNoMock: vi.fn(),
  getProblemSampleTestCasesMock: vi.fn(),
  getProblemCodeDraftMock: vi.fn(),
  runProblemCodeMock: vi.fn(),
  saveProblemCodeDraftMock: vi.fn(),
  submitProblemCodeMock: vi.fn(),
  renderMarkdownMock: vi.fn(),
  getProblemDefaultTestCasesMock: vi.fn(),
  getProblemTemplateMock: vi.fn(),
  pushMock: vi.fn(),
  successMessageMock: vi.fn(),
  warningMessageMock: vi.fn(),
  confirmMessageBoxMock: vi.fn(),
}))

vi.mock('@/api/problem', () => ({
  getProblemDetailByNo: getProblemDetailByNoMock,
  getProblemSampleTestCases: getProblemSampleTestCasesMock,
  getProblemCodeDraft: getProblemCodeDraftMock,
  runProblemCode: runProblemCodeMock,
  saveProblemCodeDraft: saveProblemCodeDraftMock,
  submitProblemCode: submitProblemCodeMock,
}))

vi.mock('@/utils/markdown', () => ({
  renderMarkdown: renderMarkdownMock,
}))

vi.mock('@/utils/problemPresets', () => ({
  getProblemDefaultTestCases: getProblemDefaultTestCasesMock,
  getProblemTemplate: getProblemTemplateMock,
}))

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    isAuthed: { value: true },
    user: { value: { username: 'demo', roleType: 'USER', roles: ['USER'], avatar: null } },
    logout: vi.fn(),
  }),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { problemNo: '1' },
    path: '/problems/1',
  }),
  useRouter: () => ({
    push: pushMock,
  }),
  RouterLink: defineComponent({
    props: {
      to: {
        type: [String, Object],
        default: '',
      },
    },
    setup(_, { slots }) {
      return () => h('a', {}, slots.default?.())
    },
  }),
}))

vi.mock('@/components/auth/LoginDialog.vue', () => ({
  default: defineComponent({
    props: {
      modelValue: {
        type: Boolean,
        default: false,
      },
    },
    setup() {
      return () => h('div')
    },
  }),
}))

vi.mock('@/components/common/UserAvatar.vue', () => ({
  default: defineComponent({
    setup() {
      return () => h('div', { class: 'user-avatar-stub' })
    },
  }),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: successMessageMock,
      warning: warningMessageMock,
      error: vi.fn(),
    },
    ElMessageBox: {
      confirm: confirmMessageBoxMock,
    },
  }
})

import ProblemSolveView from '@/views/ProblemSolveView.vue'

describe('ProblemSolveView actions', () => {
  beforeEach(() => {
    getProblemDetailByNoMock.mockReset()
    getProblemSampleTestCasesMock.mockReset()
    getProblemCodeDraftMock.mockReset()
    runProblemCodeMock.mockReset()
    saveProblemCodeDraftMock.mockReset()
    submitProblemCodeMock.mockReset()
    renderMarkdownMock.mockReset()
    getProblemDefaultTestCasesMock.mockReset()
    getProblemTemplateMock.mockReset()
    pushMock.mockReset()
    successMessageMock.mockReset()
    warningMessageMock.mockReset()
    confirmMessageBoxMock.mockReset()
    localStorage.removeItem('OJPT.solve.shortcutTipsHidden')

    getProblemDetailByNoMock.mockResolvedValue({
      data: {
        data: {
          id: '1',
          problemNo: 1,
          title: '两数之和',
          difficulty: 'EASY',
          statementMd: '### 题目描述',
          submitCount: 10,
          acceptedCount: 5,
          acceptanceRate: 50,
          tags: [],
          status: 'UNSOLVED',
          timeLimitMs: 1000,
          memoryLimitKb: 262144,
        },
      },
    })
    submitProblemCodeMock.mockResolvedValue({
      data: {
        submissionId: '9001',
        status: 'AC',
        message: '判题通过',
        timeMs: 24,
        rank: 3,
        totalCaseCount: 2,
        rankStats: {
          acceptedCount: 8,
          timeBuckets: [
            { label: '12-33 ms', min: 12, max: 33, count: 5 },
            { label: '34-55 ms', min: 34, max: 55, count: 3 },
            { label: '56-77 ms', min: 56, max: 77, count: 0 },
          ],
        },
        caseResults: [
          {
            caseIndex: 0,
            caseType: 'SAMPLE',
            status: 'AC',
            inputText: '1 2',
            expectedOutput: '3',
            actualOutput: '3\n',
            errorOutput: '',
            timeMs: 8,
            message: '通过',
          },
          {
            caseIndex: 1,
            caseType: 'HIDDEN',
            status: 'AC',
            inputText: '2 3',
            expectedOutput: '5',
            actualOutput: '5\n',
            errorOutput: '',
            timeMs: 16,
            message: '通过',
          },
        ],
      },
    })
    getProblemSampleTestCasesMock.mockResolvedValue({
      data: [{ inputText: '1 2', expectedOutput: '3', explanation: null }],
    })
    getProblemCodeDraftMock.mockResolvedValue({ data: null })
    saveProblemCodeDraftMock.mockResolvedValue({
      data: {
        problemNo: 1,
        language: 'C/C++',
        sourceCode: 'int main() {\n    return 0;\n}',
        updatedAt: '2026-04-28T13:00:00',
      },
    })
    runProblemCodeMock.mockResolvedValue({
      data: {
        status: 'FINISHED',
        caseResults: [
          {
            caseIndex: 0,
            status: 'AC',
            inputText: '1 2',
            expectedOutput: '3',
            actualOutput: '3\n',
            errorOutput: '',
            timeMs: 8,
            message: '通过',
          },
        ],
      },
    })
    renderMarkdownMock.mockReturnValue('<p>题目描述</p>')
    getProblemDefaultTestCasesMock.mockReturnValue([
      { name: 'Case 1', inputText: '1 2', outputText: '' },
    ])
    getProblemTemplateMock.mockReturnValue('int main() {\n    return 0;\n}')
    confirmMessageBoxMock.mockResolvedValue('confirm')

    vi.stubGlobal(
      'ResizeObserver',
      class {
        observe() {}
        unobserve() {}
        disconnect() {}
      },
    )
  })

  it('renders run and submit actions in the solve header', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('运行')
    expect(wrapper.text()).toContain('提交')
    expect(wrapper.text()).not.toContain('计时')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('shows code editor shortcut tips when entering the solve page', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="shortcut-tips-dialog"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Ctrl/Cmd + S')
    expect(wrapper.text()).toContain('Ctrl + Alt + L')
    expect(wrapper.text()).toContain('Ctrl/Cmd + /')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('stores the shortcut tips suppression preference when checked before closing', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="shortcut-tips-dont-show"]').setValue(true)
    await wrapper.get('[data-testid="shortcut-tips-close"]').trigger('click')
    await nextTick()

    expect(localStorage.getItem('OJPT.solve.shortcutTipsHidden')).toBe('true')
    expect(wrapper.find('[data-testid="shortcut-tips-dialog"]').exists()).toBe(false)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('does not show code editor shortcut tips after suppression is stored', async () => {
    localStorage.setItem('OJPT.solve.shortcutTipsHidden', 'true')

    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="shortcut-tips-dialog"]').exists()).toBe(false)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('renders editor status bar with language cursor sync state and line count', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editor = wrapper.get('textarea.code-editor').element as HTMLTextAreaElement
    editor.setSelectionRange(17, 17)
    await wrapper.get('textarea.code-editor').trigger('click')
    await nextTick()

    const statusBar = wrapper.get('[data-testid="code-editor-status-bar"]')
    expect(statusBar.text()).toContain('C/C++')
    expect(statusBar.text()).toContain('行 2')
    expect(statusBar.text()).toContain('列 5')
    expect(statusBar.text()).toContain('3 行')
    expect(statusBar.text()).toContain('未同步')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('opens shortcut help from the editor toolbar button', async () => {
    localStorage.setItem('OJPT.solve.shortcutTipsHidden', 'true')
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    expect(wrapper.find('[data-testid="shortcut-tips-dialog"]').exists()).toBe(false)

    await wrapper.get('[data-testid="shortcut-help-button"]').trigger('click')
    await nextTick()

    expect(wrapper.find('[data-testid="shortcut-tips-dialog"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Ctrl/Cmd + S')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('formats and comments code from editor toolbar buttons', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('int main() {\nreturn 0;   \n}')
    editor.setSelectionRange(0, editor.value.length)

    await wrapper.get('[data-testid="format-code-button"]').trigger('click')
    await nextTick()
    expect(editor.value).toBe('int main() {\n    return 0;\n}')

    editor.setSelectionRange(0, editor.value.length)
    await wrapper.get('[data-testid="comment-code-button"]').trigger('click')
    await nextTick()
    expect(editor.value).toBe('// int main() {\n    // return 0;\n// }')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('confirms before resetting code to the default template', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    await editorWrapper.setValue('custom code')
    await wrapper.get('[data-testid="reset-code-button"]').trigger('click')
    await flushPromises()

    expect(confirmMessageBoxMock).toHaveBeenCalled()
    expect((editorWrapper.element as HTMLTextAreaElement).value).toBe('int main() {\n    return 0;\n}')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('asks for confirmation before switching language while edited code is unsynced', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    confirmMessageBoxMock.mockClear()

    await wrapper.get('textarea.code-editor').setValue('custom unsynced code')
    await wrapper.get('select.language-select').setValue('Java')
    await flushPromises()

    expect(confirmMessageBoxMock).toHaveBeenCalled()

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('loads multiple backend sample cases into editable case tabs without sample/custom mode buttons', async () => {
    getProblemSampleTestCasesMock.mockResolvedValue({
      data: [
        { inputText: '1 2', expectedOutput: '3', explanation: null },
        { inputText: '4 5', expectedOutput: '9', explanation: null },
      ],
    })

    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Case 1')
    expect(wrapper.text()).toContain('Case 2')
    expect(wrapper.text()).not.toContain('题目样例')
    expect(wrapper.text()).not.toContain('我的测试')
    expect(wrapper.findAll('.testcase-tab:not(.testcase-tab--add)')).toHaveLength(2)

    const textareas = wrapper.findAll('textarea.testcase-textarea')
    expect((textareas[0].element as HTMLTextAreaElement).readOnly).toBe(false)
    expect((textareas[1].element as HTMLTextAreaElement).readOnly).toBe(false)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('allows editing the default sample input and expected output', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const textareas = wrapper.findAll('textarea.testcase-textarea')
    await textareas[0].setValue('10 20')
    await textareas[1].setValue('30')

    expect((textareas[0].element as HTMLTextAreaElement).value).toBe('10 20')
    expect((textareas[1].element as HTMLTextAreaElement).value).toBe('30')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('submits all test cases after adding a copied case', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    saveProblemCodeDraftMock.mockClear()
    runProblemCodeMock.mockClear()

    await wrapper.get('.testcase-tab--add').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="run-code-button"]').trigger('click')
    await flushPromises()

    expect(runProblemCodeMock).toHaveBeenCalledWith(expect.objectContaining({
      cases: [
        { inputText: '1 2', expectedOutput: '3' },
        { inputText: '1 2', expectedOutput: '3' },
      ],
    }))

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('falls back active case after deleting the selected case', async () => {
    getProblemSampleTestCasesMock.mockResolvedValue({
      data: [
        { inputText: '1 2', expectedOutput: '3', explanation: null },
        { inputText: '4 5', expectedOutput: '9', explanation: null },
      ],
    })

    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.findAll('.testcase-tab:not(.testcase-tab--add)')[1].trigger('click')
    await wrapper.findAll('.testcase-tab:not(.testcase-tab--add)')[1].trigger('mouseenter')
    await wrapper.get('.testcase-delete-btn').trigger('click')
    await nextTick()

    expect(wrapper.findAll('.testcase-tab:not(.testcase-tab--add)')).toHaveLength(1)
    expect(wrapper.get('.testcase-tab--active').text()).toContain('Case 1')
    expect((wrapper.findAll('textarea.testcase-textarea')[0].element as HTMLTextAreaElement).value).toBe('1 2')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('keeps expected output after run and only updates returned case statuses', async () => {
    getProblemSampleTestCasesMock.mockResolvedValue({
      data: [
        { inputText: '1 2', expectedOutput: '3', explanation: null },
        { inputText: '4 5', expectedOutput: '9', explanation: null },
        { inputText: '6 7', expectedOutput: '13', explanation: null },
      ],
    })
    runProblemCodeMock.mockResolvedValue({
      data: {
        status: 'WA',
        caseResults: [
          {
            caseIndex: 0,
            status: 'AC',
            inputText: '1 2',
            expectedOutput: '3',
            actualOutput: '999\n',
            errorOutput: '',
            timeMs: 8,
            message: '通过',
          },
          {
            caseIndex: 1,
            status: 'WA',
            inputText: '4 5',
            expectedOutput: '9',
            actualOutput: '8\n',
            errorOutput: 'wrong output',
            timeMs: 9,
            message: '输出错误',
          },
        ],
      },
    })

    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="run-code-button"]').trigger('click')
    await flushPromises()

    await wrapper.findAll('.testcase-tab:not(.testcase-tab--add)')[0].trigger('click')
    expect((wrapper.findAll('textarea.testcase-textarea')[1].element as HTMLTextAreaElement).value).toBe('3')
    expect(wrapper.findAll('.testcase-icon--success')).toHaveLength(1)
    expect(wrapper.findAll('.testcase-icon--failed')).toHaveLength(1)
    expect(wrapper.findAll('.testcase-icon--default')).toHaveLength(1)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('resets previous run statuses before running again when backend returns partial case results', async () => {
    getProblemSampleTestCasesMock.mockResolvedValue({
      data: [
        { inputText: '1 2', expectedOutput: '3', explanation: null },
        { inputText: '4 5', expectedOutput: '9', explanation: null },
      ],
    })
    runProblemCodeMock
      .mockResolvedValueOnce({
        data: {
          status: 'FINISHED',
          caseResults: [
            { caseIndex: 0, status: 'AC', inputText: '1 2', expectedOutput: '3', actualOutput: '3', errorOutput: '', timeMs: 1, message: 'ok' },
            { caseIndex: 1, status: 'AC', inputText: '4 5', expectedOutput: '9', actualOutput: '9', errorOutput: '', timeMs: 1, message: 'ok' },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          status: 'WA',
          caseResults: [
            { caseIndex: 0, status: 'WA', inputText: '1 2', expectedOutput: '3', actualOutput: '0', errorOutput: 'wrong', timeMs: 1, message: 'wrong' },
          ],
        },
      })

    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="run-code-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.testcase-icon--success')).toHaveLength(2)

    await wrapper.get('[data-testid="run-code-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.testcase-icon--failed')).toHaveLength(1)
    expect(wrapper.findAll('.testcase-icon--default')).toHaveLength(1)
    expect(wrapper.findAll('.testcase-icon--success')).toHaveLength(0)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('keeps testcase layout CSS flexible for splitter resizing', () => {
    const source = readFileSync(resolve(__dirname, '../../../src/views/ProblemSolveView.vue'), 'utf8')

    expect(source).toContain('.editor-footer {')
    expect(source).toContain('height: 100%;')
    expect(source).toContain('overflow: hidden;')
    expect(source).toContain('grid-template-rows: minmax(0, 1fr) minmax(0, 0.75fr) auto;')
    expect(source).toContain('.testcase-row {')
    expect(source).toContain('min-height: 0;')
    expect(source).toContain('height: 100%;')
    expect(source).toContain('resize: none;')
  })

  it('submits code through the backend api', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="submit-code-button"]').trigger('click')
    await flushPromises()

    expect(submitProblemCodeMock).toHaveBeenCalledWith(1, {
      language: 'C/C++',
      sourceCode: 'int main() {\n    return 0;\n}',
    })
    expect(successMessageMock).toHaveBeenCalled()
    expect(wrapper.text()).toContain('第 3 名')
    expect(wrapper.text()).toContain('通过用例')
    expect(wrapper.text()).toContain('2 / 2')
    expect(wrapper.text()).toContain('耗时分布')
    expect(wrapper.text()).not.toContain('内存分布')
    expect(wrapper.text()).not.toContain('内存占用')
    expect(wrapper.findAll('[data-testid="time-bucket-bar"]')).toHaveLength(3)
    expect(wrapper.findAll('[data-testid="submit-case-result"]')).toHaveLength(0)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('opens submit detail dialog immediately while judging', async () => {
    let resolveSubmit!: (value: unknown) => void
    submitProblemCodeMock.mockReturnValue(new Promise((resolve) => {
      resolveSubmit = resolve
    }))

    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="submit-code-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('提交详情')
    expect(wrapper.text()).toContain('判题中')

    resolveSubmit({
      data: {
        submissionId: '9002',
        status: 'WA',
        message: '答案错误',
        timeMs: 20,
        rank: null,
        totalCaseCount: 3,
        rankStats: {
          acceptedCount: 4,
          timeBuckets: [
            { label: '10-20 ms', min: 10, max: 20, count: 3 },
            { label: '21-31 ms', min: 21, max: 31, count: 1 },
          ],
        },
        caseResults: [
          {
            caseIndex: 0,
            caseType: 'SAMPLE',
            status: 'AC',
            inputText: '1 2',
            expectedOutput: '3',
            actualOutput: '3\n',
            errorOutput: '',
            timeMs: 8,
            message: '通过',
          },
          {
            caseIndex: 1,
            caseType: 'HIDDEN',
            status: 'WA',
            inputText: '2 3',
            expectedOutput: '5',
            actualOutput: '4\n',
            errorOutput: 'wrong output',
            timeMs: 20,
            message: '输出与预期不一致',
          },
        ],
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('答案错误')
    expect(wrapper.text()).toContain('1 / 3')
    expect(wrapper.text()).not.toContain('wrong output')
    expect(wrapper.text()).not.toContain('输入2 3')
    expect(wrapper.text()).not.toContain('期望输出5')
    expect(wrapper.text()).not.toContain('实际输出4')
    expect(wrapper.findAll('[data-testid="submit-case-result"]')).toHaveLength(1)
    expect(wrapper.text()).not.toContain('公开样例 1')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('runs sample cases through the backend api', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="run-code-button"]').trigger('click')
    await flushPromises()

    expect(runProblemCodeMock).toHaveBeenCalledWith({
      language: 'C/C++',
      sourceCode: 'int main() {\n    return 0;\n}',
      timeLimitMs: 1000,
      memoryLimitKb: 262144,
      cases: [{ inputText: '1 2', expectedOutput: '3' }],
    })
    expect(successMessageMock).toHaveBeenCalled()
    expect(wrapper.text()).toContain('运行完成')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('uses the slowest executed case as run elapsed time', async () => {
    getProblemSampleTestCasesMock.mockResolvedValue({
      data: [
        { inputText: 'abcabcbb', expectedOutput: '3', explanation: null },
        { inputText: 'bbbbb', expectedOutput: '1', explanation: null },
      ],
    })
    runProblemCodeMock.mockResolvedValue({
      data: {
        status: 'FINISHED',
        caseResults: [
          {
            caseIndex: 0,
            status: 'AC',
            inputText: 'abcabcbb',
            expectedOutput: '3',
            actualOutput: '3\n',
            errorOutput: '',
            timeMs: 8,
            message: '通过',
          },
          {
            caseIndex: 1,
            status: 'AC',
            inputText: 'bbbbb',
            expectedOutput: '1',
            actualOutput: '1\n',
            errorOutput: '',
            timeMs: 11,
            message: '通过',
          },
        ],
      },
    })

    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="run-code-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('11 ms')
    expect(wrapper.text()).not.toContain('19 ms')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('loads server code draft when opening the problem', async () => {
    getProblemCodeDraftMock.mockResolvedValue({
      data: {
        problemNo: 1,
        language: 'C/C++',
        sourceCode: 'int main() {\n    return 42;\n}',
        updatedAt: '2026-04-28T13:00:00',
      },
    })

    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    expect(getProblemCodeDraftMock).toHaveBeenCalledWith(1, 'C/C++')
    expect((wrapper.get('textarea.code-editor').element as HTMLTextAreaElement).value).toBe(
      'int main() {\n    return 42;\n}',
    )

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('auto saves edited code draft to the server after debounce', async () => {
    vi.useFakeTimers()
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    saveProblemCodeDraftMock.mockClear()

    await wrapper.get('textarea.code-editor').setValue('int main() {\n    return 1;\n}')
    await vi.advanceTimersByTimeAsync(900)
    await flushPromises()

    expect(saveProblemCodeDraftMock).toHaveBeenCalledTimes(1)
    expect(saveProblemCodeDraftMock).toHaveBeenCalledWith(1, {
      language: 'C/C++',
      sourceCode: 'int main() {\n    return 1;\n}',
    })

    wrapper.unmount()
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('inserts four spaces at the cursor when Tab is pressed in the code editor', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editor = wrapper.get('textarea.code-editor').element as HTMLTextAreaElement
    await wrapper.get('textarea.code-editor').setValue('abc')
    editor.setSelectionRange(1, 1)

    const event = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true })
    const preventDefaultSpy = vi.spyOn(event, 'preventDefault')
    editor.dispatchEvent(event)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('a    bc')
    expect(editor.selectionStart).toBe(5)
    expect(editor.selectionEnd).toBe(5)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('indents every selected line when Tab is pressed in the code editor', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('first\nsecond\nthird')
    editor.setSelectionRange(0, editor.value.length)

    const event = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true })
    const preventDefaultSpy = vi.spyOn(event, 'preventDefault')
    editor.dispatchEvent(event)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('    first\n    second\n    third')
    expect(editor.selectionStart).toBe(0)
    expect(editor.selectionEnd).toBe(editor.value.length)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('removes up to four leading spaces from selected lines when Shift+Tab is pressed', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('    first\n  second\nthird')
    editor.setSelectionRange(0, editor.value.length)

    const event = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true })
    const preventDefaultSpy = vi.spyOn(event, 'preventDefault')
    editor.dispatchEvent(event)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('first\nsecond\nthird')
    expect(editor.selectionStart).toBe(0)
    expect(editor.selectionEnd).toBe(editor.value.length)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('removes up to four leading spaces from the current line when Shift+Tab is pressed', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('first\n    second\nthird')
    editor.setSelectionRange(12, 12)

    const event = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true })
    const preventDefaultSpy = vi.spyOn(event, 'preventDefault')
    editor.dispatchEvent(event)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('first\nsecond\nthird')
    expect(editor.selectionStart).toBe(8)
    expect(editor.selectionEnd).toBe(8)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('saves the code draft immediately and prevents browser save on Ctrl+S', async () => {
    vi.useFakeTimers()
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    saveProblemCodeDraftMock.mockClear()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('int main() {\n    return 7;\n}')

    const event = new KeyboardEvent('keydown', { key: 's', ctrlKey: true, bubbles: true, cancelable: true })
    const preventDefaultSpy = vi.spyOn(event, 'preventDefault')
    editor.dispatchEvent(event)
    await flushPromises()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(saveProblemCodeDraftMock).toHaveBeenCalledTimes(1)
    expect(saveProblemCodeDraftMock).toHaveBeenCalledWith(1, {
      language: 'C/C++',
      sourceCode: 'int main() {\n    return 7;\n}',
    })

    await vi.advanceTimersByTimeAsync(900)
    expect(saveProblemCodeDraftMock).toHaveBeenCalledTimes(1)

    wrapper.unmount()
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('saves the code draft immediately and prevents browser save on Cmd+S', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    saveProblemCodeDraftMock.mockClear()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('int main() {\n    return 9;\n}')

    const event = new KeyboardEvent('keydown', { key: 's', metaKey: true, bubbles: true, cancelable: true })
    const preventDefaultSpy = vi.spyOn(event, 'preventDefault')
    editor.dispatchEvent(event)
    await flushPromises()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(saveProblemCodeDraftMock).toHaveBeenCalledTimes(1)
    expect(saveProblemCodeDraftMock).toHaveBeenCalledWith(1, {
      language: 'C/C++',
      sourceCode: 'int main() {\n    return 9;\n}',
    })

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('undoes and redoes code editor keyboard edits with Ctrl+Z and Ctrl+R', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('abc')
    editor.setSelectionRange(3, 3)

    const tabEvent = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true })
    editor.dispatchEvent(tabEvent)
    await nextTick()
    expect(editor.value).toBe('abc    ')

    const undoEvent = new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true, cancelable: true })
    const undoPreventDefaultSpy = vi.spyOn(undoEvent, 'preventDefault')
    editor.dispatchEvent(undoEvent)
    await nextTick()

    expect(undoPreventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('abc')
    expect(editor.selectionStart).toBe(3)
    expect(editor.selectionEnd).toBe(3)

    const redoEvent = new KeyboardEvent('keydown', { key: 'r', ctrlKey: true, bubbles: true, cancelable: true })
    const redoPreventDefaultSpy = vi.spyOn(redoEvent, 'preventDefault')
    editor.dispatchEvent(redoEvent)
    await nextTick()

    expect(redoPreventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('abc    ')
    expect(editor.selectionStart).toBe(7)
    expect(editor.selectionEnd).toBe(7)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('supports Ctrl+Y and Ctrl+Shift+Z as redo shortcuts in the code editor', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('abc')
    editor.setSelectionRange(3, 3)
    editor.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }))
    await nextTick()
    editor.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true, cancelable: true }))
    await nextTick()

    const ctrlYEvent = new KeyboardEvent('keydown', { key: 'y', ctrlKey: true, bubbles: true, cancelable: true })
    editor.dispatchEvent(ctrlYEvent)
    await nextTick()
    expect(editor.value).toBe('abc    ')

    editor.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true, cancelable: true }))
    await nextTick()
    const shiftRedoEvent = new KeyboardEvent('keydown', {
      key: 'z',
      ctrlKey: true,
      shiftKey: true,
      bubbles: true,
      cancelable: true,
    })
    editor.dispatchEvent(shiftRedoEvent)
    await nextTick()

    expect(editor.value).toBe('abc    ')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('formats code with the IntelliJ Ctrl+Alt+L shortcut and supports undo', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('int main() {\nreturn 0;   \n}')
    editor.setSelectionRange(0, editor.value.length)

    const formatEvent = new KeyboardEvent('keydown', {
      key: 'l',
      ctrlKey: true,
      altKey: true,
      bubbles: true,
      cancelable: true,
    })
    const preventDefaultSpy = vi.spyOn(formatEvent, 'preventDefault')
    editor.dispatchEvent(formatEvent)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('int main() {\n    return 0;\n}')
    expect(editor.selectionStart).toBe(0)
    expect(editor.selectionEnd).toBe(editor.value.length)

    editor.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true, cancelable: true }))
    await nextTick()

    expect(editor.value).toBe('int main() {\nreturn 0;   \n}')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('formats code with the macOS Cmd+Option+L shortcut', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('class Main {\npublic static void main(String[] args) {\nSystem.out.println(1);\n}\n}')

    const formatEvent = new KeyboardEvent('keydown', {
      key: 'l',
      metaKey: true,
      altKey: true,
      bubbles: true,
      cancelable: true,
    })
    const preventDefaultSpy = vi.spyOn(formatEvent, 'preventDefault')
    editor.dispatchEvent(formatEvent)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe(
      'class Main {\n    public static void main(String[] args) {\n        System.out.println(1);\n    }\n}',
    )

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('toggles line comments with Ctrl+Slash and supports undo', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('int a = 1;\n    return a;')
    editor.setSelectionRange(0, editor.value.length)

    const commentEvent = new KeyboardEvent('keydown', {
      key: '/',
      ctrlKey: true,
      bubbles: true,
      cancelable: true,
    })
    const preventDefaultSpy = vi.spyOn(commentEvent, 'preventDefault')
    editor.dispatchEvent(commentEvent)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('// int a = 1;\n    // return a;')

    editor.dispatchEvent(new KeyboardEvent('keydown', { key: '/', ctrlKey: true, bubbles: true, cancelable: true }))
    await nextTick()
    expect(editor.value).toBe('int a = 1;\n    return a;')

    editor.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true, cancelable: true }))
    await nextTick()
    expect(editor.value).toBe('// int a = 1;\n    // return a;')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('uses hash comments for Python when toggling line comments', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.get('select.language-select').setValue('Python3')
    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('def solve():\n    return 1')
    editor.setSelectionRange(0, editor.value.length)

    const commentEvent = new KeyboardEvent('keydown', {
      key: '/',
      ctrlKey: true,
      bubbles: true,
      cancelable: true,
    })
    const preventDefaultSpy = vi.spyOn(commentEvent, 'preventDefault')
    editor.dispatchEvent(commentEvent)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('# def solve():\n    # return 1')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('duplicates the current line with Ctrl+D', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('first\nsecond\nthird')
    editor.setSelectionRange(8, 8)

    const duplicateEvent = new KeyboardEvent('keydown', {
      key: 'd',
      ctrlKey: true,
      bubbles: true,
      cancelable: true,
    })
    const preventDefaultSpy = vi.spyOn(duplicateEvent, 'preventDefault')
    editor.dispatchEvent(duplicateEvent)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('first\nsecond\nsecond\nthird')
    expect(editor.selectionStart).toBe(15)
    expect(editor.selectionEnd).toBe(15)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('duplicates the selected text with Ctrl+D', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('abcdef')
    editor.setSelectionRange(1, 4)

    const duplicateEvent = new KeyboardEvent('keydown', {
      key: 'd',
      ctrlKey: true,
      bubbles: true,
      cancelable: true,
    })
    const preventDefaultSpy = vi.spyOn(duplicateEvent, 'preventDefault')
    editor.dispatchEvent(duplicateEvent)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('abcdbcdef')
    expect(editor.selectionStart).toBe(4)
    expect(editor.selectionEnd).toBe(7)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('deletes the current line with Ctrl+Shift+K and supports undo', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('first\nsecond\nthird')
    editor.setSelectionRange(8, 8)

    const deleteEvent = new KeyboardEvent('keydown', {
      key: 'k',
      ctrlKey: true,
      shiftKey: true,
      bubbles: true,
      cancelable: true,
    })
    const preventDefaultSpy = vi.spyOn(deleteEvent, 'preventDefault')
    editor.dispatchEvent(deleteEvent)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('first\nthird')
    expect(editor.selectionStart).toBe(6)
    expect(editor.selectionEnd).toBe(6)

    editor.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true, cancelable: true }))
    await nextTick()
    expect(editor.value).toBe('first\nsecond\nthird')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('deletes selected lines with Ctrl+Shift+K', async () => {
    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()

    const editorWrapper = wrapper.get('textarea.code-editor')
    const editor = editorWrapper.element as HTMLTextAreaElement
    await editorWrapper.setValue('first\nsecond\nthird\nfourth')
    editor.setSelectionRange(6, 18)

    const deleteEvent = new KeyboardEvent('keydown', {
      key: 'k',
      ctrlKey: true,
      shiftKey: true,
      bubbles: true,
      cancelable: true,
    })
    const preventDefaultSpy = vi.spyOn(deleteEvent, 'preventDefault')
    editor.dispatchEvent(deleteEvent)
    await nextTick()

    expect(preventDefaultSpy).toHaveBeenCalled()
    expect(editor.value).toBe('first\nfourth')
    expect(editor.selectionStart).toBe(6)
    expect(editor.selectionEnd).toBe(6)

    wrapper.unmount()
    vi.unstubAllGlobals()
  })

  it('opens run detail dialog immediately and displays the returned failed case', async () => {
    let resolveRun!: (value: unknown) => void
    const writeTextMock = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal('navigator', {
      clipboard: {
        writeText: writeTextMock,
      },
    })
    runProblemCodeMock.mockReturnValue(new Promise((resolve) => {
      resolveRun = resolve
    }))

    const wrapper = mount(ProblemSolveView, {
      attachTo: document.body,
      global: {
        plugins: [ElementPlus],
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="run-code-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="run-result-dialog"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="run-result-pending"]').exists()).toBe(true)

    resolveRun({
      data: {
        status: 'WA',
        caseResults: [
          {
            caseIndex: 0,
            caseType: 'SAMPLE',
            status: 'WA',
            inputText: 'abcabcbb',
            expectedOutput: '3',
            actualOutput: '',
            errorOutput: 'wrong output',
            timeMs: 696,
            message: 'output mismatch',
          },
        ],
      },
    })
    await flushPromises()

    expect(wrapper.findAll('[data-testid="run-case-result"]')).toHaveLength(1)
    expect(wrapper.text()).toContain('abcabcbb')
    expect(wrapper.text()).toContain('wrong output')
    expect(wrapper.text()).not.toContain('bbbbb')
    await wrapper.get('.copy-error-btn--inline').trigger('click')
    await flushPromises()
    expect(writeTextMock).toHaveBeenCalledWith('wrong output')

    wrapper.unmount()
    vi.unstubAllGlobals()
  })
})
