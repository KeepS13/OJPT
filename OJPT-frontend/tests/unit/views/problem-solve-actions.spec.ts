import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import ElementPlus from 'element-plus'

const {
  getProblemDetailByNoMock,
  getProblemSampleTestCasesMock,
  runProblemCodeMock,
  submitProblemCodeMock,
  renderMarkdownMock,
  getProblemDefaultTestCasesMock,
  getProblemTemplateMock,
  pushMock,
  successMessageMock,
  warningMessageMock,
} = vi.hoisted(() => ({
  getProblemDetailByNoMock: vi.fn(),
  getProblemSampleTestCasesMock: vi.fn(),
  runProblemCodeMock: vi.fn(),
  submitProblemCodeMock: vi.fn(),
  renderMarkdownMock: vi.fn(),
  getProblemDefaultTestCasesMock: vi.fn(),
  getProblemTemplateMock: vi.fn(),
  pushMock: vi.fn(),
  successMessageMock: vi.fn(),
  warningMessageMock: vi.fn(),
}))

vi.mock('@/api/problem', () => ({
  getProblemDetailByNo: getProblemDetailByNoMock,
  getProblemSampleTestCases: getProblemSampleTestCasesMock,
  runProblemCode: runProblemCodeMock,
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
      confirm: vi.fn(),
    },
  }
})

import ProblemSolveView from '@/views/ProblemSolveView.vue'

describe('ProblemSolveView actions', () => {
  beforeEach(() => {
    getProblemDetailByNoMock.mockReset()
    getProblemSampleTestCasesMock.mockReset()
    runProblemCodeMock.mockReset()
    submitProblemCodeMock.mockReset()
    renderMarkdownMock.mockReset()
    getProblemDefaultTestCasesMock.mockReset()
    getProblemTemplateMock.mockReset()
    pushMock.mockReset()
    successMessageMock.mockReset()
    warningMessageMock.mockReset()

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
        status: 'QUEUED',
        message: '代码已提交，等待判题',
      },
    })
    getProblemSampleTestCasesMock.mockResolvedValue({
      data: [{ inputText: '1 2', expectedOutput: '3', explanation: null }],
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

    wrapper.unmount()
    vi.unstubAllGlobals()
  })
})
