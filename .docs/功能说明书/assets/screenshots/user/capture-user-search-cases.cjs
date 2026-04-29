const path = require('path')
const { chromium } = require(path.resolve(__dirname, '../../../../../OJPT-frontend/node_modules/playwright'))

const baseUrl = 'http://127.0.0.1:8110'
const apiBase = 'http://127.0.0.1:8111'
const outputDir = __dirname

async function settle(page) {
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

async function shot(page, name) {
  await settle(page)
  await page.screenshot({
    path: path.join(outputDir, name),
    fullPage: true,
  })
}

async function loginAsUser(page) {
  const response = await page.request.post(`${apiBase}/api/auth/login`, {
    data: { account: 'user', password: '123456' },
  })
  if (!response.ok()) {
    throw new Error(`Login failed: ${response.status()} ${await response.text()}`)
  }
  const body = await response.json()
  const tokens = body.data

  await page.goto(baseUrl)
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.evaluate((data) => {
    sessionStorage.setItem('ojpt_access_token', data.accessToken)
    localStorage.setItem('ojpt_refresh_token', data.refreshToken)
  }, tokens)
  await page.reload()
  await page.waitForSelector('.nav-user', { timeout: 15000 })
}

async function clearAllFilters(page) {
  const button = page.getByTestId('clear-filters-button')
  if (await button.count()) {
    const disabled = await button.evaluate((element) => element.hasAttribute('disabled'))
    if (!disabled) {
      await button.click()
      await page.waitForTimeout(700)
    }
  }
}

async function fillProblemSearch(page, keyword) {
  const input = page.getByTestId('problem-search-input')
  await input.fill(keyword)
  await page.waitForTimeout(800)
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1000 },
    deviceScaleFactor: 1,
  })
  page.setDefaultTimeout(15000)

  await loginAsUser(page)

  await page.goto(baseUrl)
  await page.getByTestId('topnav-search-input').fill('P0001')
  await shot(page, 'user-09-topnav-search-input.png')
  await page.keyboard.press('Enter')
  await page.waitForURL(/\/problemset\?keyword=P0001/)
  await page.waitForSelector('[data-testid="problem-search-input"]')
  await shot(page, 'user-10-search-by-problem-no.png')

  await page.goto(`${baseUrl}/problemset`)
  await page.waitForSelector('[data-testid="problem-search-input"]')
  await fillProblemSearch(page, '二分')
  await shot(page, 'user-11-search-by-keyword.png')

  await clearAllFilters(page)
  await page.locator('.filter-group').nth(0).locator('button').nth(1).click()
  await page.waitForTimeout(800)
  await shot(page, 'user-12-difficulty-filter.png')

  await clearAllFilters(page)
  await page.locator('.filter-group').nth(1).locator('button').nth(1).click()
  await page.waitForTimeout(800)
  await shot(page, 'user-13-status-filter.png')

  await clearAllFilters(page)
  await page.locator('.tag-filter-row button').nth(1).click()
  await page.waitForTimeout(800)
  await shot(page, 'user-14-tag-filter.png')

  await clearAllFilters(page)
  await fillProblemSearch(page, 'P0001')
  await page.locator('.filter-group').nth(0).locator('button').nth(1).click()
  await page.waitForTimeout(500)
  await page.locator('.filter-group').nth(1).locator('button').nth(1).click()
  await page.waitForTimeout(500)
  if (await page.locator('.tag-filter-row button').count() > 1) {
    await page.locator('.tag-filter-row button').nth(1).click()
  }
  await page.waitForTimeout(800)
  await shot(page, 'user-15-combined-filters.png')

  await clearAllFilters(page)
  await fillProblemSearch(page, '不存在的题目XYZ')
  await shot(page, 'user-16-empty-search.png')

  const emptyClear = page.getByTestId('empty-clear-filters')
  if (await emptyClear.count()) {
    await emptyClear.click()
    await page.waitForTimeout(800)
  }
  await shot(page, 'user-17-clear-filters-result.png')

  await browser.close()
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
