const { chromium } = require('../../../../../OJPT-frontend/node_modules/playwright')
const path = require('path')

const BASE_URL = process.env.OJPT_FRONTEND_URL || 'http://127.0.0.1:8110'
const OUT_DIR = __dirname
const TEMP_PROBLEM_TITLE = `文档截图临时题目 ${new Date().toISOString().slice(0, 10)}`
const executablePath = process.env.PLAYWRIGHT_EXECUTABLE_PATH || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'

const shot = (name) => path.join(OUT_DIR, name)

async function waitForStable(page) {
  await page.waitForLoadState('networkidle').catch(() => {})
  await page.waitForTimeout(800)
}

async function login(page) {
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' })
  if (!(await page.getByTestId('nav-login-button').isVisible().catch(() => false))) {
    return
  }
  await page.getByTestId('nav-login-button').click()
  await page.getByTestId('login-account-input').fill('admin@qq.com')
  await page.getByTestId('login-password-input').fill('123456')
  await page.getByTestId('login-submit-button').click()
  await page.waitForURL((url) => !url.pathname.includes('login'), { timeout: 10000 }).catch(() => {})
  await page.waitForSelector('[data-testid="nav-login-button"]', { state: 'detached', timeout: 15000 })
}

async function firstVisible(locator) {
  const count = await locator.count()
  for (let i = 0; i < count; i += 1) {
    const item = locator.nth(i)
    if (await item.isVisible().catch(() => false)) return item
  }
  throw new Error('No visible locator matched')
}

async function main() {
const browser = await chromium.launch({ headless: true, executablePath })
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1000 },
    deviceScaleFactor: 1,
  })

  await login(page)

  await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-01-overview.png'), fullPage: true })

  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
  await page.waitForTimeout(500)
  await page.screenshot({ path: shot('admin-02-judge-health.png'), fullPage: false })

  await page.goto(`${BASE_URL}/admin/users`, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-03-users-list.png'), fullPage: true })

  await page.screenshot({ path: shot('admin-04-password-reset-requests.png'), fullPage: false })

  const editUserButton = await firstVisible(page.locator('[data-testid^="edit-user-"]'))
  await editUserButton.click()
  await page.getByRole('dialog', { name: '编辑用户' }).waitFor({ timeout: 10000 })
  await page.screenshot({ path: shot('admin-05-user-edit-dialog.png'), fullPage: false })
  await page.keyboard.press('Escape').catch(() => {})
  await page.getByRole('button', { name: '取消' }).last().click().catch(() => {})

  await page.goto(`${BASE_URL}/admin/problems`, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-06-problems-list.png'), fullPage: true })

  await page.getByTestId('create-problem-button').scrollIntoViewIfNeeded()
  await page.screenshot({ path: shot('admin-07-new-problem-entry.png'), fullPage: false })

  await page.getByTestId('create-problem-button').click()
  await page.waitForURL(/\/admin\/problems\/[^/]+$/, { timeout: 15000 })
  await waitForStable(page)
  await page.getByLabel('标题').fill(TEMP_PROBLEM_TITLE).catch(async () => {
    await page.locator('.problem-edit-page input').first().fill(TEMP_PROBLEM_TITLE)
  })
  await page.getByRole('button', { name: '保存' }).click()
  await page.getByText('保存成功').waitFor({ timeout: 15000 }).catch(() => {})
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-08-problem-edit-page.png'), fullPage: true })

  await page.locator('.case-block__title', { hasText: '样例测试用例' }).scrollIntoViewIfNeeded()
  await page.getByRole('button', { name: '新增样例' }).click()
  await page.getByRole('button', { name: '新增隐藏用例' }).click()
  await page.locator('.case-block').first().getByRole('textbox').first().fill('1 2')
  await page.locator('.case-block').first().getByRole('textbox').nth(1).fill('3')
  await page.locator('.case-block').nth(1).getByRole('textbox').first().fill('10 20')
  await page.locator('.case-block').nth(1).getByRole('textbox').nth(1).fill('30')
  await page.screenshot({ path: shot('admin-09-test-cases-edit.png'), fullPage: false })

  await page.goto(`${BASE_URL}/admin/tags`, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-10-tags-management.png'), fullPage: true })

  await browser.close()
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
