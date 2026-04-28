import { defaultLanguageTemplates, type SupportedLanguage } from '@/constants/languageTemplates'

export interface ProblemPresetContext {
  problemNo?: number | null
  title?: string | null
}

export interface ProblemPresetCase {
  name: string
  inputText: string
  outputText: string
}

const problemOneCases: ProblemPresetCase[] = [
  // 默认只提供样例输入，避免在做题页直接泄露答案
  { name: 'Case 1', inputText: '4\n2 7 11 15\n9', outputText: '' },
  { name: 'Case 2', inputText: '3\n3 2 4\n6', outputText: '' },
  { name: 'Case 3', inputText: '2\n3 3\n6', outputText: '' },
]

const isProblemOne = (problem: ProblemPresetContext | null | undefined) => problem?.problemNo === 1

export const getProblemTemplate = (
  problem: ProblemPresetContext | null | undefined,
  language: SupportedLanguage,
) => {
  void problem
  return defaultLanguageTemplates[language]?.template ?? ''
}

export const getProblemDefaultTestCases = (
  problem: ProblemPresetContext | null | undefined,
): ProblemPresetCase[] => {
  if (isProblemOne(problem)) {
    return problemOneCases.map((item) => ({ ...item }))
  }
  return [{ name: 'Case 1', inputText: '', outputText: '' }]
}
