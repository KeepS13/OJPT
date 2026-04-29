const path = require('path')
const { chromium } = require(path.resolve(__dirname, '../../../../../OJPT-frontend/node_modules/playwright'))

const outputDir = __dirname
const apiBase = 'http://127.0.0.1:8111'

async function main() {
  const browser = await chromium.launch({ headless: true })
  const requestContext = await browser.newContext()
  const loginResponse = await requestContext.request.post(`${apiBase}/api/auth/login`, {
    data: { account: 'user', password: '123456' },
  })

  if (!loginResponse.ok()) {
    throw new Error(`Login failed: ${loginResponse.status()} ${await loginResponse.text()}`)
  }

  const loginBody = await loginResponse.json()
  const accessToken = loginBody.data.accessToken

  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 },
    extraHTTPHeaders: {
      Authorization: `Bearer ${accessToken}`,
    },
  })

  const page = await context.newPage()
  await page.goto(`${apiBase}/api/auth/me`, { waitUntil: 'networkidle' })
  await page.screenshot({
    path: path.join(outputDir, 'auth-08-current-user-info.png'),
    fullPage: true,
  })

  await browser.close()
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
