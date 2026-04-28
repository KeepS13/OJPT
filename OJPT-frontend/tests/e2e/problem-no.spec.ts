import { test, expect } from '@playwright/test'

const TEST_EMAIL_USER = 'user@qq.com'
const TEST_PASSWORD = '123456'

async function loginAsStudent(page: any) {
  await page.goto('/')

  await page.getByTestId('nav-login-button').click()
  await page.getByTestId('login-account-input').fill(TEST_EMAIL_USER)
  await page.getByTestId('login-password-input').fill(TEST_PASSWORD)
  await page.getByTestId('login-submit-button').click()

  await expect(page.getByText('登录成功')).toBeVisible()
}

test('题库列表页和详情页应显示格式化后的 problemNo (P0001)', async ({ page, request }) => {
  await loginAsStudent(page)

  // 访问题库页
  await page.goto('/problemset')
  await expect(page.getByRole('heading', { name: '题库' })).toBeVisible()

  // 等待题目列表加载
  await expect(page.locator('.problem-row').first()).toBeVisible({ timeout: 10000 })

  // 验证列表第一行的题号显示为 P0001 格式（四位，前面补0）
  const firstRowNumber = page.locator('.problem-row').first().locator('.col-id')
  await expect(firstRowNumber).toContainText('P0001')

  // 验证第二题题号为 P0002
  const secondRowNumber = page.locator('.problem-row').nth(1).locator('.col-id')
  await expect(secondRowNumber).toContainText('P0002')

  // 点击第一题进入详情页
  const firstRowLink = page.locator('.problem-row').first().locator('.problem-link')
  await firstRowLink.click()

  // 等待导航完成，URL 使用短题号
  await page.waitForURL(/\/problems\/1/)

  // 验证详情页标题显示 "P0001. 两数之和" 格式
  // 题目名称通常在 h1 或带有 .title-main 类的元素中
  const titleElement = page.locator('.title-main, h1, .problem-title').first()
  await expect(titleElement).toContainText('P0001. 两数之和')

  // 验证 URL 使用短题号，便于直接访问和分享
  const currentUrl = page.url()
  expect(currentUrl).toContain('/problems/1')
})

test('题库移除排序控件后仍按题号默认展示', async ({ page }) => {
  await loginAsStudent(page)

  await page.goto('/problemset')

  await expect(page.locator('#orderBy')).toHaveCount(0)
  await expect(page.getByText('排序：')).toHaveCount(0)
  await expect(page.locator('.problem-row').first()).toBeVisible({ timeout: 10000 })

  const firstRowNumber = page.locator('.problem-row').first().locator('.col-id')
  await expect(firstRowNumber).toContainText('P0001')

  const secondRowNumber = page.locator('.problem-row').nth(1).locator('.col-id')
  await expect(secondRowNumber).toContainText('P0002')
})
