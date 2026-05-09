import { chromium } from '../../../../../OJPT-frontend/node_modules/playwright/index.mjs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const outDir = dirname(fileURLToPath(import.meta.url))
const frontBase = 'http://127.0.0.1:8110'
const backBase = 'http://127.0.0.1:8111'
const executablePath = process.env.PLAYWRIGHT_EXECUTABLE_PATH || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'

const browser = await chromium.launch({ headless: true, executablePath })
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })

async function capture(name, options = {}) {
  await page.waitForTimeout(options.delay ?? 700)
  await page.screenshot({
    path: join(outDir, name),
    fullPage: options.fullPage ?? false,
  })
}

async function loginAsAdmin() {
  await page.goto(`${frontBase}/`, { waitUntil: 'networkidle' })
  await page.getByTestId('nav-login-button').click()
  await page.getByTestId('login-account-input').fill('admin@qq.com')
  await page.getByTestId('login-password-input').fill('123456')
  await page.getByTestId('login-submit-button').click()
  await page.waitForTimeout(1200)
}

await page.goto(`${frontBase}/`, { waitUntil: 'networkidle' })
await capture('overview-01-home.png', { fullPage: true })

await page.goto(`${frontBase}/problemset`, { waitUntil: 'networkidle' })
await capture('overview-02-problemset-entry.png', { fullPage: true })

await page.goto(`${backBase}/actuator/health`, { waitUntil: 'networkidle' })
await capture('overview-03-actuator-health.png')

await page.goto(`${backBase}/swagger-ui/index.html`, { waitUntil: 'networkidle' })
await page.waitForSelector('.swagger-ui', { timeout: 10000 })
await capture('overview-04-swagger-ui.png', { delay: 1500 })

await loginAsAdmin()
await page.goto(`${frontBase}/admin`, { waitUntil: 'networkidle' })
await capture('overview-05-admin-dashboard.png', { fullPage: true })

await browser.close()
