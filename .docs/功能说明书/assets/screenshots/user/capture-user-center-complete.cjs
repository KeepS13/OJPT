const fs = require('fs')
const path = require('path')
const { chromium } = require(path.resolve(__dirname, '../../../../../OJPT-frontend/node_modules/playwright'))

const baseUrl = 'http://127.0.0.1:8110'
const apiBase = 'http://127.0.0.1:8111'
const outputDir = __dirname
const avatarPath = path.join(outputDir, 'user-temp-avatar.png')
let accessToken = ''

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
  await page.waitForTimeout(800)
}

async function shotPage(page, name) {
  await settle(page)
  await page.screenshot({ path: path.join(outputDir, name), fullPage: true })
}

async function shotLocator(page, locator, name) {
  await settle(page)
  await locator.scrollIntoViewIfNeeded().catch(() => {})
  await page.waitForTimeout(300)
  await locator.screenshot({ path: path.join(outputDir, name) })
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
  accessToken = tokens.accessToken

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

async function getDetail(page) {
  const res = await page.request.get(`${apiBase}/api/users/me/detail`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  if (!res.ok()) throw new Error(`Get detail failed: ${res.status()} ${await res.text()}`)
  const body = await res.json()
  return body.data
}

async function restoreProfile(page, original) {
  const payload = {
    gender: original.gender ?? 0,
    birthday: original.birthday || null,
    address: original.address || null,
    website: original.website || null,
    github: original.github || null,
    company: original.company || null,
    position: original.position || null,
    bio: original.bio || null,
  }
  await page.request.put(`${apiBase}/api/users/me`, {
    data: payload,
    headers: { Authorization: `Bearer ${accessToken}` },
  }).catch(() => {})
}

async function ensureTempAvatar() {
  if (fs.existsSync(avatarPath)) return
  const pngBase64 =
    'iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAbElEQVR4nO3QQQ3AIADAQMC/5+ECjiYKinRddpLu3jOA+nOvA7AcoDkAcwDmAMwBmAMwB2AOwByAOQBzAOYAzAGYAzAHYA7AHIA5AHMA5gDMAZgDMAdoDsAcgDkAcwDmAMwBmAMwB2AOwByAOQBzADcSKQJE6vijKAAAAABJRU5ErkJggg=='
  fs.writeFileSync(avatarPath, Buffer.from(pngBase64, 'base64'))
}

async function closeDialog(page) {
  await page.keyboard.press('Escape')
  await page.waitForTimeout(400)
}

async function main() {
  await ensureTempAvatar()

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1000 },
    deviceScaleFactor: 1,
  })
  page.setDefaultTimeout(15000)

  await page.goto(baseUrl)
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.reload()
  await page.waitForSelector('.home-view, main, .login-btn')
  await shotPage(page, 'user-18-home-before-login.png')

  await loginAsUser(page)
  const originalProfile = await getDetail(page)

  try {
    await page.goto(baseUrl)
    await page.waitForSelector('.nav-user')
    await shotPage(page, 'user-19-home-after-login.png')
    await page.locator('.nav-user').hover()
    await page.waitForSelector('.user-menu')
    await shotPage(page, 'user-20-user-menu-profile-security.png')

    await page.goto(`${baseUrl}/problemset`)
    await page.waitForSelector('.problem-table tbody tr.problem-row')
    await shotPage(page, 'user-21-problemset-before-open-problem.png')
    const firstProblemLink = page.locator('.problem-link').first()
    await firstProblemLink.click()
    await page.waitForURL(/\/problems\/\d+/)
    await page.waitForSelector('.problem-solve-page, .solve-page, .code-editor, [data-testid="run-code-button"]')
    await shotPage(page, 'user-22-problem-solve-after-click.png')

    await page.goto(`${baseUrl}/problemset`)
    await page.waitForSelector('.problem-table')
    const nextButton = page.locator('.pagination-bar button.btn-next, .pagination-bar .btn-next').first()
    if (await nextButton.count()) {
      await nextButton.click()
      await page.waitForTimeout(1000)
    }
    await shotPage(page, 'user-23-problemset-page-switched.png')
    const sizeDropdown = page.locator('.pagination-bar .el-select').first()
    if (await sizeDropdown.count()) {
      await sizeDropdown.click()
      await page.waitForSelector('.el-select-dropdown:visible')
      await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: '50' }).first().click()
      await page.waitForTimeout(1000)
    }
    await shotPage(page, 'user-24-problemset-page-size-50.png')

    await page.goto(`${baseUrl}/profile`)
    await page.waitForSelector('.profile-view')
    await shotPage(page, 'user-25-profile-before-edit.png')
    const addressInput = page.locator('.profile-form input').nth(5)
    const companyInput = page.locator('.profile-form input').nth(11)
    const positionInput = page.locator('.profile-form input').nth(12)
    const bioInput = page.locator('.profile-form textarea').first()
    await addressInput.fill('Doc Capture Temp Address')
    await companyInput.fill('OJPT Docs Temp')
    await positionInput.fill('Screenshot User')
    await bioInput.fill('Temporary profile text for screenshots.')
    await shotPage(page, 'user-26-profile-edited-fields-before-save.png')
    await page.locator('.form-actions .el-button--primary').click()
    await page.waitForSelector('.el-message')
    await shotPage(page, 'user-27-profile-save-success-message.png')
    await addressInput.fill('Reset Preview Address')
    await shotPage(page, 'user-28-profile-before-reset.png')
    await page.locator('.form-actions .el-button').nth(1).click()
    await page.waitForTimeout(1000)
    await shotPage(page, 'user-29-profile-after-reset.png')

    await shotLocator(page, page.locator('.profile-avatar-section'), 'user-30-profile-avatar-upload-entry.png')
    await page.locator('input[type="file"]').setInputFiles(avatarPath)
    await page.waitForSelector('.el-message')
    await shotPage(page, 'user-31-profile-avatar-upload-success.png')
    await shotLocator(page, page.locator('.profile-avatar-section'), 'user-32-profile-avatar-delete-action.png')
    const deleteAvatarButton = page.locator('.avatar-delete-btn').first()
    if (await deleteAvatarButton.count()) {
      await deleteAvatarButton.click({ force: true })
      await page.waitForSelector('.el-message')
    }
    await shotPage(page, 'user-33-profile-avatar-delete-result.png')

    await page.goto(`${baseUrl}/profile/training`)
    await page.waitForSelector('.training-dashboard-view')
    await shotLocator(page, page.locator('.metrics-grid'), 'user-34-training-metric-cards.png')
    await shotLocator(page, page.locator('.dashboard-grid .panel-card').nth(0), 'user-35-training-status-distribution.png')
    await shotLocator(page, page.locator('.dashboard-grid .panel-card').nth(1), 'user-36-training-difficulty-distribution.png')
    await shotLocator(page, page.locator('.recent-card'), 'user-37-training-recent-submissions.png')

    await page.goto(`${baseUrl}/profile/submissions`)
    await page.waitForSelector('.submission-records-view')
    const submissionNext = page.locator('.submission-records-view .btn-next').first()
    if (await submissionNext.count() && !(await submissionNext.isDisabled().catch(() => true))) {
      await submissionNext.click()
      await page.waitForTimeout(1000)
    } else {
      const submissionSize = page.locator('.submission-records-view .el-select').first()
      if (await submissionSize.count()) {
        await submissionSize.click()
        await page.waitForSelector('.el-select-dropdown:visible')
        await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: '20' }).first().click()
        await page.waitForTimeout(1000)
      }
    }
    await shotPage(page, 'user-38-submissions-page-switched.png')
    const viewCodeButton = page.locator('.submission-records-view .el-table .el-button').first()
    if (await viewCodeButton.count()) {
      await viewCodeButton.click()
      await page.waitForSelector('.el-dialog:visible')
    }
    await shotPage(page, 'user-39-submission-code-dialog.png')
    await shotLocator(page, page.locator('.el-dialog:visible .code-dialog'), 'user-40-submission-compile-judge-info.png')
    await closeDialog(page)

    await page.goto(`${baseUrl}/profile/security`)
    await page.waitForSelector('.security-view')
    const editButtons = page.locator('.security-content .edit-btn')
    await editButtons.nth(0).click()
    await page.waitForSelector('.el-dialog:visible')
    await shotPage(page, 'user-41-security-change-username-dialog.png')
    await closeDialog(page)
    await editButtons.nth(1).click()
    await page.waitForSelector('.el-dialog:visible')
    await shotPage(page, 'user-42-security-change-email-dialog.png')
    await closeDialog(page)
    await editButtons.nth(2).click()
    await page.waitForSelector('.el-dialog:visible')
    await shotPage(page, 'user-43-security-change-phone-dialog.png')
    await closeDialog(page)
    await editButtons.nth(3).click()
    await page.waitForSelector('.el-dialog:visible')
    await page.locator('.el-dialog:visible input').nth(0).fill('wrong-password')
    await page.locator('.el-dialog:visible input').nth(1).fill('abc')
    await page.locator('.el-dialog:visible input').nth(2).fill('abcd')
    await page.locator('.el-dialog:visible .el-button--primary').click()
    await page.waitForTimeout(800)
    await shotPage(page, 'user-44-security-password-validation-error.png')
    await closeDialog(page)
    await page.locator('.delete-btn').click()
    await page.waitForSelector('.el-dialog:visible')
    await shotPage(page, 'user-45-security-delete-first-confirm.png')
    await page.locator('.el-dialog:visible .el-button--danger').click()
    await page.waitForTimeout(500)
    await page.waitForSelector('.el-dialog:visible')
    await shotPage(page, 'user-46-security-delete-second-confirm.png')
  } finally {
    await restoreProfile(page, originalProfile)
    await page.request.post(`${apiBase}/api/users/me/avatar`, {
      multipart: {},
      headers: { Authorization: `Bearer ${accessToken}` },
    }).catch(() => {})
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
