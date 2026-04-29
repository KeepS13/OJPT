const { chromium } = require('../../../../../OJPT-frontend/node_modules/playwright')
const path = require('path')

const baseUrl = 'http://127.0.0.1:8110'
const outDir = __dirname

async function waitForApp(page) {
  await page.goto(baseUrl, { waitUntil: 'networkidle' })
  await page.waitForSelector('[data-testid="nav-login-button"], .nav-user', { timeout: 15000 })
}

async function clearSession(page) {
  await page.goto(baseUrl, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.context().clearCookies()
}

async function openLogin(page) {
  await waitForApp(page)
  await page.getByTestId('nav-login-button').click()
  await page.locator('.login-dialog').waitFor({ state: 'visible', timeout: 10000 })
}

async function loginAdmin(page) {
  await clearSession(page)
  await openLogin(page)
  await page.getByTestId('login-account-input').fill('admin@qq.com')
  await page.getByTestId('login-password-input').fill('123456')
  await page.getByTestId('login-submit-button').click()
  await page.waitForSelector('.nav-user', { timeout: 15000 })
}

async function submitPasswordResetRequest(page) {
  await clearSession(page)
  await openLogin(page)
  await page.locator('.action-links a').last().click()
  await page.getByTestId('password-reset-account-input').fill('user@qq.com')
  await page.getByTestId('password-reset-submit-button').click()
  await page.locator('.el-dialog').last().waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {})
}

async function getPendingRowCount(page) {
  const table = page.locator('.table-section .el-table').first()
  await table.waitFor({ state: 'visible', timeout: 15000 })
  await page.waitForTimeout(800)
  const emptyVisible = await table.locator('.el-table__empty-block').isVisible().catch(() => false)
  if (emptyVisible) return 0
  return table.locator('.el-table__body-wrapper tbody tr').count()
}

async function screenshotRegion(page, name, region) {
  await page.screenshot({
    path: path.join(outDir, name),
    clip: region,
    animations: 'disabled',
  })
}

async function getTableRegion(page) {
  return page.evaluate(() => {
    const section = document.querySelector('.table-section')
    const header = section?.querySelector('.section-header')
    const table = section?.querySelector('.el-table')
    if (!section || !header || !table) throw new Error('password reset table not found')

    const headerRect = header.getBoundingClientRect()
    const tableRect = table.getBoundingClientRect()
    const left = Math.max(0, Math.min(headerRect.left, tableRect.left) - 8)
    const top = Math.max(0, headerRect.top - 8)
    const right = Math.min(window.innerWidth, Math.max(headerRect.right, tableRect.right) + 8)
    const bottom = Math.min(window.innerHeight, tableRect.bottom + 8)
    return { x: left, y: top, width: right - left, height: bottom - top }
  })
}

async function getActionsRegion(page) {
  return page.evaluate(() => {
    const table = document.querySelector('.table-section .el-table')
    const row = table?.querySelector('.el-table__fixed-right tbody tr') || table?.querySelector('tbody tr')
    if (!table || !row) throw new Error('password reset action row not found')

    const tableRect = table.getBoundingClientRect()
    const rowRect = row.getBoundingClientRect()
    const left = Math.max(0, tableRect.right - 360)
    const top = Math.max(0, rowRect.top - 42)
    const right = Math.min(window.innerWidth, tableRect.right + 8)
    const bottom = Math.min(window.innerHeight, rowRect.bottom + 20)
    return { x: left, y: top, width: right - left, height: bottom - top }
  })
}

async function capture(page) {
  await loginAdmin(page)
  await page.goto(`${baseUrl}/admin/users`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.table-section .el-table', { timeout: 15000 })

  if ((await getPendingRowCount(page)) === 0) {
    await submitPasswordResetRequest(page)
    await loginAdmin(page)
    await page.goto(`${baseUrl}/admin/users`, { waitUntil: 'networkidle' })
    await page.waitForSelector('.table-section .el-table', { timeout: 15000 })
  }

  if ((await getPendingRowCount(page)) === 0) {
    throw new Error('no pending password reset request is visible')
  }

  await page.setViewportSize({ width: 1366, height: 768 })
  await page.waitForTimeout(500)
  await screenshotRegion(page, 'auth-06-admin-password-reset-list.png', await getTableRegion(page))
  await screenshotRegion(page, 'auth-07-admin-password-reset-actions.png', await getActionsRegion(page))
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1366, height: 768 }, deviceScaleFactor: 1 })
  try {
    await capture(page)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
