import { chromium } from '../../../../../OJPT-frontend/node_modules/playwright/index.mjs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const apiOutDir = dirname(fileURLToPath(import.meta.url))
const overviewOutDir = resolve(apiOutDir, '../overview')
const authOutDir = resolve(apiOutDir, '../auth')
const backBase = process.env.OJPT_BACKEND_URL ?? 'http://127.0.0.1:8111'

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })

const twoSumPython = `import sys
data = list(map(int, sys.stdin.read().split()))
n = data[0]
nums = data[1:1 + n]
target = data[1 + n]

seen = {}
for i, value in enumerate(nums):
    other = target - value
    if other in seen:
        print(seen[other], i)
        break
    seen[value] = i
`

function maskToken(value) {
  if (typeof value !== 'string') {
    return value
  }
  if (!value) {
    return value
  }
  return `${value.slice(0, 18)}...（已截断）`
}

function sanitize(value, key = '') {
  const loweredKey = key.toLowerCase()
  if (value == null) {
    return value
  }
  if (loweredKey.includes('password')) {
    return '******'
  }
  if (loweredKey.includes('token')) {
    return maskToken(value)
  }
  if (loweredKey === 'authorization' && typeof value === 'string') {
    return value.replace(/^Bearer\s+(.+)$/i, (_, token) => `Bearer ${maskToken(token)}`)
  }
  if (Array.isArray(value)) {
    return value.map((item) => sanitize(item))
  }
  if (typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([entryKey, entryValue]) => [
      entryKey,
      sanitize(entryValue, entryKey),
    ]))
  }
  return value
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function stringify(value) {
  return JSON.stringify(sanitize(value), null, 2)
}

async function requestApi({ title, method = 'GET', path, body, token }) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${backBase}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await response.text()
  let parsed
  try {
    parsed = JSON.parse(text)
  } catch {
    parsed = text
  }

  return {
    raw: parsed,
    doc: {
      title,
      request: {
        method,
        url: `${backBase}${path}`,
        headers: sanitize(headers),
        body: body ?? null,
        capturedAt: new Date().toISOString(),
      },
      status: response.status,
      ok: response.ok,
      response: parsed,
    },
  }
}

async function renderJsonScreenshot(doc, outputPath) {
  const statusClass = doc.ok ? 'ok' : 'error'
  const html = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <style>
    :root {
      color-scheme: light;
      --bg: #f4f6fb;
      --panel: #ffffff;
      --border: #d8dee9;
      --text: #0f172a;
      --muted: #64748b;
      --ok: #16794c;
      --error: #b42318;
    }
    * {
      box-sizing: border-box;
    }
    body {
      margin: 0;
      padding: 30px;
      background: var(--bg);
      color: var(--text);
      font-family: "Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", Arial, sans-serif;
    }
    h1 {
      margin: 0 0 16px;
      font-size: 26px;
      line-height: 1.3;
      font-weight: 700;
      letter-spacing: 0;
    }
    .meta {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 12px;
      color: var(--muted);
      font-size: 14px;
    }
    .badge {
      display: inline-flex;
      align-items: center;
      border: 1px solid var(--border);
      border-radius: 6px;
      padding: 4px 8px;
      background: #fff;
      font-weight: 600;
    }
    .badge.ok {
      color: var(--ok);
      border-color: #9bd3b5;
      background: #f0fdf4;
    }
    .badge.error {
      color: var(--error);
      border-color: #f1aaa3;
      background: #fff1f0;
    }
    .panel {
      border: 1px solid var(--border);
      border-radius: 8px;
      background: var(--panel);
      box-shadow: 0 12px 26px rgb(15 23 42 / 0.08);
      overflow: hidden;
    }
    pre {
      margin: 0;
      padding: 22px 26px;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      word-break: break-word;
      font-family: Consolas, "JetBrains Mono", "Courier New", monospace;
      font-size: 14px;
      line-height: 1.52;
      color: #111827;
    }
  </style>
</head>
<body>
  <h1>${escapeHtml(doc.title)}</h1>
  <div class="meta">
    <span class="badge">${escapeHtml(doc.request.method)}</span>
    <span class="badge ${statusClass}">HTTP ${doc.status}</span>
    <span class="badge ${statusClass}">${doc.ok ? '请求成功' : '请求失败'}</span>
  </div>
  <div class="panel">
    <pre>${escapeHtml(stringify(doc))}</pre>
  </div>
</body>
</html>`

  await page.setContent(html, { waitUntil: 'load' })
  await page.screenshot({ path: outputPath, fullPage: true })
}

async function captureApi(name, options) {
  const { doc, raw } = await requestApi(options)
  await renderJsonScreenshot(doc, join(apiOutDir, name))
  return raw
}

async function captureApiWhen(name, options, predicate, attempts = 4) {
  let lastResult
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    lastResult = await requestApi(options)
    if (predicate(lastResult.raw)) {
      break
    }
    await page.waitForTimeout(1500)
  }
  await renderJsonScreenshot(lastResult.doc, join(apiOutDir, name))
  return lastResult.raw
}

async function captureStandaloneJson(outputPath, options) {
  const { doc, raw } = await requestApi(options)
  await renderJsonScreenshot(doc, outputPath)
  return raw
}

const userLogin = await requestApi({
  title: 'POST /api/auth/login 登录接口响应',
  method: 'POST',
  path: '/api/auth/login',
  body: { account: 'user@qq.com', password: '123456' },
})
await renderJsonScreenshot(userLogin.doc, join(apiOutDir, 'api-01-login.png'))
const userToken = userLogin.raw?.data?.accessToken

const adminLogin = await requestApi({
  title: 'POST /api/auth/login 管理员登录接口响应',
  method: 'POST',
  path: '/api/auth/login',
  body: { account: 'admin@qq.com', password: '123456' },
})
const adminToken = adminLogin.raw?.data?.accessToken

const uniqueSuffix = Date.now()
await captureApi('api-02-register.png', {
  title: 'POST /api/auth/register 注册接口响应',
  method: 'POST',
  path: '/api/auth/register',
  body: {
    account: `doc_api_${uniqueSuffix}@example.com`,
    password: '123456',
    nickname: '文档截图用户',
    gender: 1,
    birthday: '2000-01-01',
  },
})

await captureApi('api-03-current-user.png', {
  title: 'GET /api/auth/me 当前用户接口响应',
  path: '/api/auth/me',
  token: userToken,
})

await captureApi('api-04-problem-list.png', {
  title: 'GET /api/problems 题库列表接口响应',
  path: '/api/problems?page=1&size=5',
})

await captureApi('api-05-problem-detail.png', {
  title: 'GET /api/problems/no/1 题目详情接口响应',
  path: '/api/problems/no/1',
})

await captureApi('api-06-run-code.png', {
  title: 'POST /api/problems/run 运行代码接口响应',
  method: 'POST',
  path: '/api/problems/run',
  token: userToken,
  body: {
    language: 'Python3',
    sourceCode: twoSumPython,
    timeLimitMs: 2000,
    memoryLimitKb: 262144,
    cases: [
      {
        inputText: '4\n2 7 11 15\n9',
        expectedOutput: '0 1',
      },
    ],
  },
})

await captureApi('api-07-submit-code.png', {
  title: 'POST /api/problems/no/1/submissions 提交代码接口响应',
  method: 'POST',
  path: '/api/problems/no/1/submissions',
  token: userToken,
  body: {
    language: 'Python3',
    sourceCode: twoSumPython,
  },
})

await captureApi('api-08-admin-users.png', {
  title: 'GET /api/admin/users 管理员用户列表接口响应',
  path: '/api/admin/users?page=1&size=5',
  token: adminToken,
})

await captureApi('api-09-admin-problems.png', {
  title: 'GET /api/admin/problems 管理员题目列表接口响应',
  path: '/api/admin/problems?page=1&size=5',
  token: adminToken,
})

await captureApiWhen('api-10-judge-health.png', {
  title: 'GET /api/admin/judge-environment/health 判题环境健康接口响应',
  path: '/api/admin/judge-environment/health',
  token: adminToken,
}, (raw) => raw?.data?.status === 'UP')

await captureStandaloneJson(join(authOutDir, 'auth-08-current-user-info.png'), {
  title: 'GET /api/auth/me 当前用户信息接口响应',
  path: '/api/auth/me',
  token: userToken,
})

await captureStandaloneJson(join(authOutDir, 'auth-22-auth-me-response-with-token.png'), {
  title: 'GET /api/auth/me 携带 Token 的当前用户信息接口响应',
  path: '/api/auth/me',
  token: userToken,
})

await captureStandaloneJson(join(overviewOutDir, 'overview-03-actuator-health.png'), {
  title: 'GET /actuator/health Actuator 健康检查',
  path: '/actuator/health',
})

await page.goto(`${backBase}/swagger-ui/index.html`, { waitUntil: 'networkidle' })
await page.waitForSelector('.swagger-ui', { timeout: 10000 })
await page.waitForTimeout(1800)
await page.screenshot({
  path: join(overviewOutDir, 'overview-04-swagger-ui.png'),
  fullPage: false,
})

await browser.close()
