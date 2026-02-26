import { test, expect } from '@playwright/test'

const TEST_EMAIL = 'admin@qq.com'
const TEST_PASSWORD = '123456'

async function loginAsAdmin(page: import('@playwright/test').Page) {
  await page.goto('/')

  await page.getByTestId('nav-login-button').click()
  await page.getByTestId('login-account-input').fill(TEST_EMAIL)
  await page.getByTestId('login-password-input').fill(TEST_PASSWORD)
  await page.getByTestId('login-submit-button').click()

  await expect(page.getByText('登录成功')).toBeVisible()
}

test('管理员登录后可以访问后台数据概览看板', async ({ page }) => {
  await loginAsAdmin(page)

  // 直接访问后台首页路由，会触发路由守卫中的角色校验与统计接口加载
  await page.goto('/admin')

  // 顶部标题应显示“数据概览”
  await expect(page.getByRole('heading', { name: '数据概览' })).toBeVisible()

  // OverviewTab 中的几个核心分区标题应可见，验证统计卡片区域已加载
  await expect(page.getByText('平台统计')).toBeVisible()
  await expect(page.getByText('用户统计')).toBeVisible()
  await expect(page.getByText('学校统计')).toBeVisible()
}
)

