import { test, expect } from '@playwright/test'

const ADMIN_EMAIL = 'admin@qq.com'
const PASSWORD = '123456'

// 来自 Flyway 预置数据：V1_1__init_user_data.sql
const ONLY_SCHOOL_EMAIL = 'only_school@qq.com'
const ONLY_SCHOOL_USER_ID = '1998338632572506119'

async function loginApi(request: import('@playwright/test').APIRequestContext, account: string) {
  const res = await request.post('http://localhost:8080/api/auth/login', {
    headers: { 'Content-Type': 'application/json' },
    data: { account, password: PASSWORD },
  })
  expect(res.status()).toBe(200)
  const body = await res.json()
  const accessToken = body?.data?.accessToken as string | undefined
  expect(accessToken).toBeTruthy()
  return accessToken as string
}

async function blacklistUser(
  request: import('@playwright/test').APIRequestContext,
  accessToken: string,
  userId: string,
  durationSeconds: number
) {
  const res = await request.post(
    `http://localhost:8080/api/admin/users/${userId}/blacklist?durationSeconds=${durationSeconds}`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    }
  )
  expect(res.status()).toBe(200)
  const body = await res.json()
  expect(body?.code).toBe(200)
}

async function unblacklistUser(
  request: import('@playwright/test').APIRequestContext,
  accessToken: string,
  userId: string
) {
  const res = await request.delete(`http://localhost:8080/api/admin/users/${userId}/blacklist`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  })
  expect(res.status()).toBe(200)
  const body = await res.json()
  expect(body?.code).toBe(200)
}

test('封禁 only_school 后，前端登录应提示封禁剩余时间', async ({ page, request }) => {
  const adminAccessToken = await loginApi(request, ADMIN_EMAIL)

  // 先确保处于“未封禁”状态，避免历史数据影响
  await unblacklistUser(request, adminAccessToken, ONLY_SCHOOL_USER_ID)

  // 封禁 120s：测试时足够稳定，也不会影响太久
  await blacklistUser(request, adminAccessToken, ONLY_SCHOOL_USER_ID, 120)

  try {
    await page.goto('/')
    await page.getByTestId('nav-login-button').click()

    await page.getByTestId('login-account-input').fill(ONLY_SCHOOL_EMAIL)
    await page.getByTestId('login-password-input').fill(PASSWORD)
    await page.getByTestId('login-submit-button').click()

    const toast = page.locator('.el-message--error')
    await expect(toast).toContainText('账号已被封禁')
    await expect(toast).toContainText('剩余')
  } finally {
    // 清理：恢复账号，避免影响其他用例/手工联调
    await unblacklistUser(request, adminAccessToken, ONLY_SCHOOL_USER_ID)
  }
})

