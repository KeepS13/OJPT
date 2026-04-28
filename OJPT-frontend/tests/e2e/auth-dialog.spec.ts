import { expect, test, type Page } from '@playwright/test'

const PASSWORD = '123456'

async function openAuthDialog(page: Page) {
  await page.goto('/')
  await page.getByTestId('nav-login-button').click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await expect(page.getByTestId('login-account-input')).toBeVisible()
  await expect(page.getByTestId('login-submit-button')).toBeEnabled()
}

async function capture(page: Page, name: string) {
  await page.waitForTimeout(150)
  const screenshot = await page.screenshot({
    path: `test-results/auth/${name}.png`,
  })
  expect(screenshot.length).toBeGreaterThan(1000)
}

async function expectLoggedInMenu(page: Page, displayName: string) {
  await expect(page.getByTestId('nav-login-button')).toBeHidden()
  await page.locator('.nav-user').hover()
  await expect(page.locator('.user-menu__name')).toContainText(displayName)
}

test('email login signs in and leaves a verified screenshot', async ({ page }) => {
  await openAuthDialog(page)
  await capture(page, 'email-login-form')

  await page.getByTestId('login-account-input').fill('admin@qq.com')
  await page.getByTestId('login-password-input').fill(PASSWORD)
  await page.getByTestId('login-submit-button').click()

  await expect(page.getByText('登录成功')).toBeVisible()
  await expectLoggedInMenu(page, 'admin')
  await capture(page, 'email-login-success')
})

test('phone login signs in with seeded phone number and leaves a verified screenshot', async ({ page }) => {
  await openAuthDialog(page)
  await page.getByRole('button', { name: '手机号登录' }).click()

  await page.getByTestId('login-account-input').fill('13800000003')
  await page.getByTestId('login-password-input').fill(PASSWORD)
  await page.getByTestId('login-submit-button').click()

  await expect(page.getByText('登录成功')).toBeVisible()
  await expectLoggedInMenu(page, 'user')
  await capture(page, 'phone-login-success')
})

test('email registration requires profile fields and can register a new account', async ({ page }) => {
  const stamp = Date.now()
  const nickname = `e2e_email_${String(stamp).slice(-8)}`

  await openAuthDialog(page)
  await page.getByTestId('switch-register-link').click()
  await capture(page, 'email-register-empty-form')

  await page.getByTestId('login-submit-button').click()
  await expect(page.getByText('请输入邮箱')).toBeVisible()
  await expect(page.getByText('请输入昵称')).toBeVisible()
  await expect(page.getByText('请选择性别')).toBeVisible()
  await expect(page.getByPlaceholder('选择生日')).toHaveCount(0)
  await capture(page, 'email-register-required-errors')

  await page.getByTestId('login-account-input').fill(`${nickname}@example.com`)
  await page.getByTestId('login-password-input').fill(PASSWORD)
  await page.getByTestId('register-nickname-input').fill(nickname)
  await page.getByText('男').click()
  await page.getByTestId('login-submit-button').click()

  await expect(page.getByText('注册成功')).toBeVisible()
  await expectLoggedInMenu(page, nickname)
  await capture(page, 'email-register-success')
})

test('phone registration can register a new account and then auto login', async ({ page }) => {
  const stamp = String(Date.now())
  const phone = `17${stamp.slice(-9)}`
  const nickname = `e2e_phone_${stamp.slice(-8)}`

  await openAuthDialog(page)
  await page.getByTestId('switch-register-link').click()
  await page.getByRole('button', { name: '手机号注册' }).click()
  await capture(page, 'phone-register-form')

  await page.getByTestId('login-account-input').fill(phone)
  await page.getByTestId('login-password-input').fill(PASSWORD)
  await page.getByTestId('register-nickname-input').fill(nickname)
  await page.getByText('女').click()
  await page.getByTestId('login-submit-button').click()

  await expect(page.getByText('注册成功')).toBeVisible()
  await expectLoggedInMenu(page, nickname)
  await capture(page, 'phone-register-success')
})
