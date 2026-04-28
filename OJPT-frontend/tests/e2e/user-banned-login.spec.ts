import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@qq.com'
const PASSWORD = '123456'

async function loginApi(request: import('@playwright/test').APIRequestContext, account: string) {
  const res = await request.post('http://localhost:8111/api/auth/login', {
    headers: { 'Content-Type': 'application/json' },
    data: { account, password: PASSWORD },
  })
  expect(res.status()).toBe(200)
  const body = await res.json()
  const accessToken = body?.data?.accessToken as string | undefined
  expect(accessToken).toBeTruthy()
  return accessToken as string
}

async function registerUserApi(request: import('@playwright/test').APIRequestContext) {
  const stamp = String(Date.now())
  const account = `ban_e2e_${stamp}@example.com`
  const res = await request.post('http://localhost:8111/api/auth/register', {
    headers: { 'Content-Type': 'application/json' },
    data: {
      account,
      password: PASSWORD,
      nickname: `ban_e2e_${stamp.slice(-8)}`,
      gender: 1,
      birthday: '2000-01-01',
    },
  })
  expect(res.status()).toBe(200)
  const rawBody = await res.text()
  const body = JSON.parse(rawBody)
  expect(body?.code).toBe(200)
  const userId = rawBody.match(/"userId"\s*:\s*"?(\d+)"?/)?.[1]
  expect(userId).toBeTruthy()
  return {
    account,
    userId: userId as string,
  }
}

async function updateUserStatus(
  request: import('@playwright/test').APIRequestContext,
  accessToken: string,
  userId: string,
  status: number
) {
  const res = await request.put(
    `http://localhost:8111/api/admin/users/${userId}/status`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      data: { status },
    }
  )
  expect(res.status()).toBe(200)
  const body = await res.json()
  expect(body?.code).toBe(200)
}

test('禁用用户后，前端登录应提示账号不可用', async ({ page, request }) => {
  const adminAccessToken = await loginApi(request, ADMIN_EMAIL)
  const targetUser = await registerUserApi(request)

  await updateUserStatus(request, adminAccessToken, targetUser.userId, 0)

  try {
    await page.goto('/')
    await page.getByTestId('nav-login-button').click()

    await page.getByTestId('login-account-input').fill(targetUser.account)
    await page.getByTestId('login-password-input').fill(PASSWORD)
    await page.getByTestId('login-submit-button').click()

    const toast = page.locator('.el-message--error')
    await expect(toast).toContainText(/账号不可用|帐号已被锁定|账号已被锁定/)
  } finally {
    // 清理：恢复账号，避免影响其他用例/手工联调
    await updateUserStatus(request, adminAccessToken, targetUser.userId, 1)
  }
})
