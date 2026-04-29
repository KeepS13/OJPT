const path = require('path')
const { chromium } = require(path.resolve(__dirname, '../../../../../OJPT-frontend/node_modules/playwright'))

const baseUrl = 'http://127.0.0.1:8110'
const outputDir = __dirname

async function waitForSettled(page) {
  await page.waitForLoadState('domcontentloaded')
  await page.addStyleTag({
    content: `
      #vue-inspector-container,
      #__vue-devtools-container__,
      [class*="vue-devtools"] {
        display: none !important;
        visibility: hidden !important;
      }
    `,
  }).catch(() => {})
  await page.waitForTimeout(900)
}

async function screenshot(page, name) {
  await waitForSettled(page)
  await page.screenshot({
    path: path.join(outputDir, name),
    fullPage: true,
  })
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1000 },
    deviceScaleFactor: 1,
  })

  page.setDefaultTimeout(15000)

  const loginResponse = await page.request.post('http://127.0.0.1:8111/api/auth/login', {
    data: { account: 'user', password: '123456' },
  })
  if (!loginResponse.ok()) {
    throw new Error(`Login failed: ${loginResponse.status()} ${await loginResponse.text()}`)
  }
  const loginBody = await loginResponse.json()
  const loginData = loginBody.data

  await page.goto(baseUrl)
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.evaluate((tokens) => {
    sessionStorage.setItem('ojpt_access_token', tokens.accessToken)
    localStorage.setItem('ojpt_refresh_token', tokens.refreshToken)
  }, loginData)
  await page.reload()
  await page.waitForSelector('.nav-user', { timeout: 15000 })

  await page.goto(`${baseUrl}/`)
  await screenshot(page, 'user-01-home.png')

  await page.goto(`${baseUrl}/problemset`)
  await page.waitForSelector('.problemset-page, .problem-table, table')
  await screenshot(page, 'user-02-problemset.png')

  await page.goto(`${baseUrl}/profile`)
  await page.waitForSelector('.profile-page, .profile-content')
  await screenshot(page, 'user-03-profile.png')

  await page.goto(`${baseUrl}/profile/training`)
  await page.waitForSelector('.training-page, .dashboard-page, .training-content, main')
  await screenshot(page, 'user-04-training-dashboard.png')

  await page.goto(`${baseUrl}/profile/submissions`)
  await page.waitForSelector('.submission-page, .submissions-page, .submission-records, main')
  await screenshot(page, 'user-05-submissions.png')

  await page.goto(`${baseUrl}/profile/security`)
  await page.waitForSelector('.security-page, .security-content')
  await screenshot(page, 'user-06-security.png')

  const editButtons = page.locator('.security-content .edit-btn')
  if (await editButtons.count() >= 4) {
    await editButtons.nth(3).click()
    await page.waitForTimeout(500)
    await screenshot(page, 'user-07-change-password-dialog.png')
    await page.keyboard.press('Escape')
    await page.waitForTimeout(300)
  }

  await page.goto(`${baseUrl}/profile/security`)
  await page.waitForSelector('.security-page, .security-content')
  const deleteButton = page.locator('.delete-btn')
  if (await deleteButton.count()) {
    await deleteButton.first().click()
    await page.waitForTimeout(500)
    await screenshot(page, 'user-08-delete-account-confirm.png')
  }

  await browser.close()
}

main().catch(async (error) => {
  console.error(error)
  process.exit(1)
})
