import { test, expect } from '@playwright/test'

// 约定：后端数据库中预置一个测试账号
// 邮箱：test@example.com（或你实际配置的测试邮箱）
// 密码：123456
const TEST_EMAIL = 'admin@qq.com'
const TEST_PASSWORD = '123456'

test('用户可以通过顶部登录弹窗成功登录', async ({ page }) => {
  await page.goto('/')

  // 打开登录弹窗
  await page.getByTestId('nav-login-button').click()

  // 填写账号和密码（默认邮箱登录）
  await page.getByTestId('login-account-input').fill(TEST_EMAIL)
  await page.getByTestId('login-password-input').fill(TEST_PASSWORD)

  // 提交登录
  await page.getByTestId('login-submit-button').click()

  // 断言：出现登录成功提示
  await expect(page.getByText('登录成功')).toBeVisible()
})

