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

test('管理员可以进入学校管理列表并看到表格', async ({ page }) => {
  await loginAsAdmin(page)

  await page.goto('/admin/schools')

  // AdminLayout 顶部标题
  await expect(page.getByRole('heading', { name: '学校管理' })).toBeVisible()

  // 筛选区与表格关键列名（用于确认页面已渲染）
  await expect(page.getByPlaceholder('搜索学校名称')).toBeVisible()
  await expect(page.getByRole('button', { name: '创建学校' })).toBeVisible()

  const tableHead = page.locator('thead')
  await expect(tableHead.getByText('学校名称', { exact: true })).toBeVisible()
  await expect(tableHead.getByText('状态', { exact: true })).toBeVisible()
})

