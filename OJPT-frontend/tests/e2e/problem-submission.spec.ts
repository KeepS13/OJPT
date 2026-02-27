import { test, expect } from '@playwright/test'

const TEST_EMAIL_USER = 'test_user@qq.com'
const TEST_PASSWORD = '123456'

async function loginAsStudent(page: import('@playwright/test').Page) {
  await page.goto('/')

  await page.getByTestId('nav-login-button').click()
  await page.getByTestId('login-account-input').fill(TEST_EMAIL_USER)
  await page.getByTestId('login-password-input').fill(TEST_PASSWORD)
  await page.getByTestId('login-submit-button').click()

  await expect(page.getByText('登录成功')).toBeVisible()
}

test('学员可以浏览题库并完成一次提交（QUEUED）', async ({ page, request }) => {
  await loginAsStudent(page)

  // 打开题库页，验证基础布局渲染正常
  await page.goto('/problemset')
  await expect(page.getByRole('heading', { name: '题库' })).toBeVisible()

  // 直接访问一个示例题目的做题页（前端当前为静态题面）
  await page.goto('/problems/2100000000000000001')
  await expect(page.getByText('两数之和')).toBeVisible()

  // 通过后端登录接口获取 accessToken（避免前端状态同步时序问题）
  const loginRes = await request.post('http://localhost:8080/api/auth/login', {
    headers: {
      'Content-Type': 'application/json',
    },
    data: {
      account: TEST_EMAIL_USER,
      password: TEST_PASSWORD,
    },
  })
  expect(loginRes.status()).toBe(200)
  const loginBody = await loginRes.json()
  const accessToken = loginBody?.data?.accessToken as string | undefined
  expect(accessToken).toBeTruthy()

  // 调用后端提交接口，验证 stub 判题链路可用
  const res = await request.post('http://localhost:8080/api/submissions', {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    data: {
      // 注意：后端 ID 为雪花长整型，JS number 会丢精度，必须用字符串
      problemId: '2100000000000000001',
      language: 'C++',
      sourceCode: 'int main(){return 0;}',
    },
  })

  const status = res.status()
  if (status !== 200) {
    const text = await res.text()
    console.log('submission_status', status)
    console.log('submission_body', text)
  }
  expect(status).toBe(200)
  const body = await res.json()

  // 按后端统一返回结构校验：code=200 且 data.status 为 QUEUED
  expect(body?.code).toBe(200)
  expect(body?.data?.status).toBe('QUEUED')
})

