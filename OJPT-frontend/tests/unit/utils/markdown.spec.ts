import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '@/utils/markdown'

describe('renderMarkdown', () => {
  it('renders basic markdown elements', () => {
    const md = '# 标题\\n\\n这是一个 **加粗** 文本，包含 `code`。'
    const html = renderMarkdown(md)

    expect(html).toContain('<h1')
    expect(html).toContain('标题')
    expect(html).toContain('<strong>')
    expect(html).toContain('<code>')
  })

  it('sanitizes dangerous script tags', () => {
    const md = '正常文本<script>alert(\"xss\")</script>'
    const html = renderMarkdown(md)

    expect(html.toLowerCase()).not.toContain('<script')
  })

  it('sanitizes javascript: urls', () => {
    const md = '[链接](javascript:alert(1))'
    const html = renderMarkdown(md)

    expect(html.toLowerCase()).not.toContain('javascript:')
  })
})

