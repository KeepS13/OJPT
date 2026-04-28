import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@qq.com'
const USER_EMAIL = 'test_user@qq.com'
const PASSWORD = '123456'

async function uiLogin(page: import('@playwright/test').Page, account: string) {
  await page.goto('/')
  await page.getByTestId('nav-login-button').click()
  await page.getByTestId('login-account-input').fill(account)
  await page.getByTestId('login-password-input').fill(PASSWORD)
  await page.getByTestId('login-submit-button').click()
  await expect(page.getByText('登录成功')).toBeVisible()
}

test('草稿→管理端发布→学员端可见', async ({ page, request }) => {
  // 1) 用普通用户 token 创建草稿
  const loginRes = await request.post('http://localhost:8111/api/auth/login', {
    headers: { 'Content-Type': 'application/json' },
    data: { account: USER_EMAIL, password: PASSWORD },
  })
  expect(loginRes.status()).toBe(200)
  const loginBody = await loginRes.json()
  const userAccessToken = loginBody?.data?.accessToken as string
  expect(userAccessToken).toBeTruthy()

  const title = `e2e-draft-${Date.now()}`
  const createRes = await request.post('http://localhost:8111/api/problems', {
    headers: {
      Authorization: `Bearer ${userAccessToken}`,
      'Content-Type': 'application/json',
    },
    data: {
      title,
      difficulty: 'EASY',
      statementMd: '## E2E\n\n- 输入：`1`\n- 输出：`1`\n',
      timeLimitMs: 1000,
      memoryLimitKb: 256000,
    },
  })
  expect(createRes.status()).toBe(200)
  const createBody = await createRes.json()
  const problemId = String(createBody?.data?.id)
  expect(problemId).toBeTruthy()

  // 2) 管理员 UI 发布
  await uiLogin(page, ADMIN_EMAIL)
  await page.goto('/admin/problems')

  // 搜索刚创建的草稿
  await page.getByPlaceholder('按标题搜索').fill(title)
  await page.getByRole('button', { name: '搜索' }).click()

  // 点击发布
  await page.getByRole('button', { name: '发布' }).first().click()
  await expect(page.getByText('发布成功')).toBeVisible()

  // 3) 学员端可见
  await page.goto('/problemset')
  await expect(page.getByRole('link', { name: title })).toBeVisible()
})

