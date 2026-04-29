const { chromium } = require('../../../../../OJPT-frontend/node_modules/playwright')
const path = require('path')

const baseUrl = 'http://127.0.0.1:8110'
const outDir = __dirname
const stamp = Date.now().toString().slice(-8)
const email = `authdoc${stamp}@example.com`
const password = 'Authdoc123456'
const nickname = `authdoc${stamp}`

async function waitForApp(page) {
  await page.goto(baseUrl, { waitUntil: 'networkidle' })
  await page.waitForSelector('[data-testid="nav-login-button"], .nav-user', { timeout: 15000 })
}

async function screenshot(page, name) {
  await page.screenshot({
    path: path.join(outDir, name),
    fullPage: true,
    animations: 'disabled',
  })
}

async function waitMessagesGone(page) {
  await page.locator('.el-message').first().waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {})
}

async function openLogin(page) {
  await page.getByTestId('nav-login-button').click()
  await page.locator('.login-dialog').waitFor({ state: 'visible', timeout: 10000 })
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1366, height: 768 }, deviceScaleFactor: 1 })

  await page.context().clearCookies()
  await waitForApp(page)
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await waitForApp(page)

  await openLogin(page)
  await screenshot(page, 'auth-01-login-dialog.png')

  await page.getByTestId('switch-register-link').click()
  await page.getByTestId('login-account-input').fill(email)
  await page.getByTestId('login-password-input').fill(password)
  await page.getByTestId('register-nickname-input').fill(nickname)
  await page.locator('[data-testid="register-gender-radio"] label').first().click()
  await screenshot(page, 'auth-02-register-form.png')

  await page.getByTestId('login-submit-button').click()
  await page.waitForSelector('.nav-user', { timeout: 15000 })
  await waitMessagesGone(page)
  await page.locator('.nav-user').dispatchEvent('mouseenter')
  await page.locator('.user-menu').waitFor({ state: 'visible', timeout: 10000 })
  await page.waitForTimeout(300)
  await screenshot(page, 'auth-03-login-success-user-menu.png')

  await page.locator('.user-menu__logout').click()
  await page.waitForSelector('[data-testid="nav-login-button"]', { timeout: 15000 })
  await waitMessagesGone(page)
  await screenshot(page, 'auth-05-after-logout.png')

  await openLogin(page)
  if (await page.getByTestId('switch-login-link').isVisible().catch(() => false)) {
    await page.getByTestId('switch-login-link').click()
  }
  await page.locator('.action-links a').last().click()
  await page.getByTestId('password-reset-account-input').fill(email)
  await page.locator('.el-dialog').filter({ hasText: '忘记密码' }).last().waitFor({ state: 'visible' })
  await screenshot(page, 'auth-04-password-reset-request.png')
  await page.getByTestId('password-reset-submit-button').click()
  await page.waitForTimeout(800)

  await browser.close()
  console.log(JSON.stringify({ email, password, nickname }, null, 2))
}

main().catch(async (error) => {
  console.error(error)
  process.exit(1)
})
