import { describe, expect, it } from 'vitest'
import { getProblemDefaultTestCases, getProblemTemplate } from '../../../src/utils/problemPresets'
import { defaultLanguageTemplates } from '../../../src/constants/languageTemplates'

describe('problem presets', () => {
  it('returns blank starter templates instead of full solutions for problem no 1', () => {
    const cppTemplate = getProblemTemplate({ problemNo: 1 }, 'C/C++')
    const javaTemplate = getProblemTemplate({ problemNo: 1 }, 'Java')
    const pythonTemplate = getProblemTemplate({ problemNo: 1 }, 'Python3')

    expect(cppTemplate).toBe(defaultLanguageTemplates['C/C++'].template)
    expect(javaTemplate).toBe(defaultLanguageTemplates.Java.template)
    expect(pythonTemplate).toBe(defaultLanguageTemplates.Python3.template)

    expect(cppTemplate).not.toContain('cin >> n;')
    expect(javaTemplate).not.toContain('Scanner scanner')
    expect(pythonTemplate).not.toContain('input().split()')
  })

  it('returns sample inputs for problem no 1 without pre-filling expected outputs', () => {
    const cases = getProblemDefaultTestCases({ problemNo: 1 })

    expect(cases).toHaveLength(3)
    expect(cases[0]).toMatchObject({
      inputText: '4\n2 7 11 15\n9',
      outputText: '',
    })
    expect(cases.every((item) => item.outputText === '')).toBe(true)
  })
})
