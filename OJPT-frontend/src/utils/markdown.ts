import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 简单的 Markdown 渲染 + XSS 过滤封装
// 在浏览器环境下使用 DOMPurify 进行安全过滤
export function renderMarkdown(source: string): string {
  if (!source) return ''

  // 先用 marked 转为 HTML 字符串
  const rawHtml = marked.parse(source, {
    breaks: true,
    gfm: true,
  }) as string

  // 在浏览器环境下用 DOMPurify 做 XSS 过滤
  if (typeof window !== 'undefined') {
    return DOMPurify.sanitize(rawHtml, {
      USE_PROFILES: { html: true },
    })
  }

  // 非浏览器环境（例如测试时的 jsdom）直接返回 rawHtml，交由测试环境控制
  return rawHtml
}

