const { chromium } = require('../../../../../OJPT-frontend/node_modules/playwright')
const path = require('path')

const baseUrl = 'http://127.0.0.1:8110'
const apiUrl = 'http://127.0.0.1:8111/api'
const outDir = __dirname

const stamp = Date.now().toString().slice(-9)
const suffix = `doc_auth_${stamp}`
const password = 'DocAuth123456'
const wrongPassword = 'Wrong123456'
const emailAccount = `${suffix}@example.com`
const phoneAccount = `13${stamp.padStart(9, '0').slice(0, 9)}`
const approvedEmail = `${suffix}_approved@example.com`
const rejectedEmail = `${suffix}_rejected@example.com`

async function api(pathname, options = {}) {
  const response = await fetch(`${apiUrl}${pathname}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok || (body && body.code && body.code !== 200)) {
    throw new Error(`${options.method || 'GET'} ${pathname} failed: ${response.status} ${JSON.stringify(body)}`)
  }
  return body.data
}

async function loginApi(account, pwd = password) {
  return api('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ account, password: pwd }),
  })
}

async function registerApi(account, nickname) {
  return api('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ account, password, nickname, gender: 1 }),
  })
}

async function requestReset(account) {
  await api('/auth/password-reset-requests', {
    method: 'POST',
    body: JSON.stringify({ account }),
  })
}

async function getResetRequests(token, status) {
  return api(`/admin/password-reset-requests?status=${status}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
}

async function screenshot(page, name, options = {}) {
  await page.screenshot({
    path: path.join(outDir, name),
    fullPage: true,
    animations: 'disabled',
    ...options,
  })
}

async function waitForApp(page) {
  await page.goto(baseUrl, { waitUntil: 'networkidle' })
  await page.waitForSelector('[data-testid="nav-login-button"], .nav-user', { timeout: 15000 })
}

async function clearSession(page) {
  await page.context().clearCookies()
  await page.goto(baseUrl, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
}

async function openLogin(page) {
  await waitForApp(page)
  await page.getByTestId('nav-login-button').click()
  await page.locator('.login-dialog').waitFor({ state: 'visible', timeout: 10000 })
}

async function waitMessage(page, text) {
  const msg = page.locator('.el-message').filter({ hasText: text }).last()
  await msg.waitFor({ state: 'visible', timeout: 10000 })
  await page.waitForTimeout(250)
}

async function waitMessagesGone(page) {
  await page.locator('.el-message').first().waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {})
}

async function loginUi(page, account, pwd = password) {
  await clearSession(page)
  await openLogin(page)
  await page.getByTestId('login-account-input').fill(account)
  await page.getByTestId('login-password-input').fill(pwd)
  await page.getByTestId('login-submit-button').click()
  await page.waitForSelector('.nav-user', { timeout: 15000 })
}

async function logoutIfNeeded(page) {
  if (await page.locator('.nav-user').isVisible().catch(() => false)) {
    await page.locator('.nav-user').dispatchEvent('mouseenter')
    await page.locator('.user-menu').waitFor({ state: 'visible', timeout: 10000 })
    await page.locator('.user-menu__logout').click()
    await page.waitForSelector('[data-testid="nav-login-button"]', { timeout: 15000 })
    await waitMessagesGone(page)
  }
}

async function captureLoginBranches(page) {
  await clearSession(page)
  await openLogin(page)
  await page.getByTestId('login-account-input').fill(emailAccount)
  await page.getByTestId('login-password-input').fill(password)
  await screenshot(page, 'auth-11-login-form-filled.png')

  await page.getByTestId('login-submit-button').click()
  await waitMessage(page, '登录成功')
  await screenshot(page, 'auth-12-login-success-toast.png')
  await waitMessagesGone(page)

  await logoutIfNeeded(page)
  await openLogin(page)
  await page.getByTestId('login-account-input').fill(emailAccount)
  await page.getByTestId('login-password-input').fill(wrongPassword)
  await page.getByTestId('login-submit-button').click()
  await waitMessage(page, '用户名或密码错误')
  await screenshot(page, 'auth-13-login-wrong-password-error.png')
  await waitMessagesGone(page)

  await page.locator('.login-tabs .tab').filter({ hasText: '手机号登录' }).click()
  await page.getByTestId('login-account-input').fill(phoneAccount)
  await page.getByTestId('login-password-input').fill(password)
  await screenshot(page, 'auth-14-phone-login-filled.png')
  await page.getByTestId('login-submit-button').click()
  await waitMessage(page, '登录成功')
  await screenshot(page, 'auth-15-phone-login-success.png')
  await waitMessagesGone(page)
  await logoutIfNeeded(page)

  await openLogin(page)
  await page.getByTestId('login-account-input').fill(`${suffix}@`)
  await page.getByTestId('login-account-input').press('ArrowDown')
  await page.waitForSelector('.email-suggest', { timeout: 5000 })
  await screenshot(page, 'auth-16-email-suggestion-dropdown.png')
}

async function captureRegisterBranches(page) {
  await clearSession(page)
  await openLogin(page)
  await page.getByTestId('switch-register-link').click()
  await page.getByTestId('login-submit-button').click()
  await page.waitForSelector('.field-error', { timeout: 5000 })
  await screenshot(page, 'auth-17-register-required-validation.png')

  const uiEmail = `${suffix}_ui@example.com`
  await page.getByTestId('login-account-input').fill(uiEmail)
  await page.getByTestId('login-password-input').fill(password)
  await page.getByTestId('register-nickname-input').fill(`${suffix}_ui`)
  await page.locator('[data-testid="register-gender-radio"] label').first().click()
  await screenshot(page, 'auth-18-email-register-filled.png')
  await page.getByTestId('login-submit-button').click()
  await waitMessage(page, '注册成功')
  await screenshot(page, 'auth-20-register-success-auto-login.png')
  await waitMessagesGone(page)
  await logoutIfNeeded(page)

  await openLogin(page)
  await page.getByTestId('switch-register-link').click()
  await page.locator('.login-tabs .tab').filter({ hasText: '手机号注册' }).click()
  const uiPhone = `15${stamp.padStart(9, '0').slice(0, 9)}`
  await page.getByTestId('login-account-input').fill(uiPhone)
  await page.getByTestId('login-password-input').fill(password)
  await page.getByTestId('register-nickname-input').fill(`${suffix}_phone_ui`)
  await page.locator('[data-testid="register-gender-radio"] label').last().click()
  await screenshot(page, 'auth-19-phone-register-filled.png')
}

async function captureSession(page) {
  await loginUi(page, emailAccount)
  await page.goto(`${baseUrl}/profile/security`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.security-view, .nav-user', { timeout: 15000 })
  await page.reload({ waitUntil: 'networkidle' })
  await page.waitForSelector('.nav-user', { timeout: 15000 })
  await screenshot(page, 'auth-21-protected-page-session-restored.png')

  const token = await page.evaluate(() => sessionStorage.getItem('ojpt_access_token'))
  const context = await page.context().browser().newContext({
    viewport: { width: 1366, height: 768 },
    extraHTTPHeaders: { Authorization: `Bearer ${token}` },
  })
  const apiPage = await context.newPage()
  await apiPage.goto(`${apiUrl}/auth/me`, { waitUntil: 'networkidle' })
  await screenshot(apiPage, 'auth-22-auth-me-response-with-token.png')
  await context.close()
}

async function captureResetApprovals(page) {
  const admin = await loginApi('admin', '123456')

  await requestReset(approvedEmail)
  await loginUi(page, 'admin@qq.com', '123456')
  await page.goto(`${baseUrl}/admin/users`, { waitUntil: 'networkidle' })
  const approvedRow = page.locator('.table-section .el-table').first().locator('tbody tr', { hasText: approvedEmail }).first()
  await approvedRow.waitFor({ state: 'visible', timeout: 15000 })
  await approvedRow.getByRole('button', { name: '同意' }).click()
  await waitMessage(page, '密码已重置为默认 123456')
  await screenshot(page, 'auth-23-password-reset-approve-toast.png')
  await waitMessagesGone(page)

  await requestReset(rejectedEmail)
  await page.goto(`${baseUrl}/admin/users`, { waitUntil: 'networkidle' })
  const rejectedRow = page.locator('.table-section .el-table').first().locator('tbody tr', { hasText: rejectedEmail }).first()
  await rejectedRow.waitFor({ state: 'visible', timeout: 15000 })
  await rejectedRow.getByRole('button', { name: '拒绝' }).click()
  await waitMessage(page, '已拒绝重置申请')
  await screenshot(page, 'auth-24-password-reset-reject-toast.png')
  await waitMessagesGone(page)

  const approved = await getResetRequests(admin.accessToken, 'APPROVED')
  if (!approved.some((item) => item.email === approvedEmail || item.accountIdentifier === approvedEmail)) {
    throw new Error('approved temp reset request not found')
  }
  const rejected = await getResetRequests(admin.accessToken, 'REJECTED')
  if (!rejected.some((item) => item.email === rejectedEmail || item.accountIdentifier === rejectedEmail)) {
    throw new Error('rejected temp reset request not found')
  }

  const context = await page.context().browser().newContext({
    viewport: { width: 1366, height: 768 },
    extraHTTPHeaders: { Authorization: `Bearer ${admin.accessToken}` },
  })
  const approvedPage = await context.newPage()
  await approvedPage.goto(`${apiUrl}/admin/password-reset-requests?status=APPROVED`, { waitUntil: 'networkidle' })
  await screenshot(approvedPage, 'auth-25-password-reset-approved-list.png')
  const rejectedPage = await context.newPage()
  await rejectedPage.goto(`${apiUrl}/admin/password-reset-requests?status=REJECTED`, { waitUntil: 'networkidle' })
  await screenshot(rejectedPage, 'auth-26-password-reset-rejected-list.png')
  await context.close()
}

async function captureLogout(page) {
  await loginUi(page, emailAccount)
  await page.locator('.nav-user').dispatchEvent('mouseenter')
  await page.locator('.user-menu').waitFor({ state: 'visible', timeout: 10000 })
  await screenshot(page, 'auth-27-user-menu-before-logout.png')
  await page.locator('.user-menu__logout').click()
  await page.waitForSelector('[data-testid="nav-login-button"]', { timeout: 15000 })
  await screenshot(page, 'auth-28-logout-anonymous-state.png')
}

async function main() {
  await registerApi(emailAccount, suffix)
  await registerApi(phoneAccount, `${suffix}_phone`)
  await registerApi(approvedEmail, `${suffix}_approved`)
  await registerApi(rejectedEmail, `${suffix}_rejected`)

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1366, height: 768 }, deviceScaleFactor: 1 })
  try {
    await captureLoginBranches(page)
    await captureRegisterBranches(page)
    await captureSession(page)
    await captureResetApprovals(page)
    await captureLogout(page)
  } finally {
    await browser.close()
  }

  console.log(JSON.stringify({
    emailAccount,
    phoneAccount,
    approvedEmail,
    rejectedEmail,
  }, null, 2))
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
