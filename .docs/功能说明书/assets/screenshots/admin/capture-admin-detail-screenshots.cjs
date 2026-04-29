const { chromium } = require('../../../../../OJPT-frontend/node_modules/playwright')
const path = require('path')

const BASE_URL = process.env.OJPT_FRONTEND_URL || 'http://127.0.0.1:8110'
const API_URL = process.env.OJPT_BACKEND_URL || 'http://127.0.0.1:8111/api'
const OUT_DIR = __dirname
const STAMP = String(Date.now()).slice(-8)
const PREFIX = `doc_admin_${STAMP}`

const shot = (name) => path.join(OUT_DIR, name)

function parseJsonPreserveIds(text) {
  return JSON.parse(text.replace(/"([^"]*[Ii][Dd])"\s*:\s*(\d{15,})/g, (_, key, id) => `"${key}":"${id}"`))
}

async function api(pathname, options = {}, token) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(`${API_URL}${pathname}`, {
    ...options,
    headers,
  })
  const text = await res.text()
  const body = text ? parseJsonPreserveIds(text) : null
  if (!res.ok || (body && body.code && body.code !== 200)) {
    throw new Error(`${options.method || 'GET'} ${pathname} failed: ${res.status} ${text}`)
  }
  return body ? body.data : undefined
}

async function loginApi(account, password) {
  return api('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ account, password }),
  })
}

async function registerUser(nickname, account) {
  await api('/auth/register', {
    method: 'POST',
    body: JSON.stringify({
      account,
      password: '123456',
      nickname,
      gender: 1,
      birthday: '2000-01-01',
    }),
  }).catch((error) => {
    if (!String(error.message).includes('已被使用')) throw error
  })
}

async function getAdminToken() {
  const candidates = ['admin', 'admin@qq.com']
  let lastError
  for (const account of candidates) {
    try {
      const data = await loginApi(account, '123456')
      return data.accessToken
    } catch (error) {
      lastError = error
    }
  }
  throw lastError
}

async function userByKeyword(token, keyword) {
  const data = await api(`/admin/users?page=1&size=10&keyword=${encodeURIComponent(keyword)}`, {}, token)
  return (data.records || [])[0]
}

async function ensureUser(token, suffix, status = 1) {
  const nickname = `${PREFIX}_${suffix}`
  const account = `${nickname}@example.com`
  await registerUser(nickname, account)
  const user = await userByKeyword(token, nickname)
  if (!user) throw new Error(`Cannot find temp user ${nickname}`)
  await api(`/admin/users/${user.userId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  }, token)
  return { ...user, account, nickname }
}

async function createProblem(token, title, status = 'DRAFT') {
  const created = await api('/admin/problems', {
    method: 'POST',
    body: JSON.stringify({
      title,
      difficulty: 'EASY',
      statementMd: '## 题目描述\n\n用于管理员功能说明书截图的临时题目。\n\n## 输入\n\n两个整数。\n\n## 输出\n\n它们的和。',
      timeLimitMs: 1000,
      memoryLimitKb: 256000,
    }),
  }, token)
  if (status === 'PUBLISHED') {
    await api(`/admin/problems/${created.id}:publish`, { method: 'POST' }, token)
  } else if (status === 'ARCHIVED') {
    await api(`/admin/problems/${created.id}:archive`, { method: 'POST' }, token)
  }
  return created.id
}

async function createTag(token, name, type = '文档截图') {
  return api('/admin/tags', {
    method: 'POST',
    body: JSON.stringify({ name, type }),
  }, token).catch(async () => {
    const tags = await api('/admin/tags', {}, token)
    return tags.find((tag) => tag.name === name)
  })
}

async function submitReset(account) {
  await api('/auth/password-reset-requests', {
    method: 'POST',
    body: JSON.stringify({ account }),
  })
}

async function waitForStable(page) {
  await page.waitForLoadState('networkidle').catch(() => {})
  await page.waitForTimeout(700)
}

async function loginUi(page, account = 'admin') {
  const candidates = account === 'admin'
    ? ['admin', 'admin@qq.com']
    : account === 'user'
      ? ['user', 'user@qq.com']
      : [account]
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  if (!(await page.getByTestId('nav-login-button').isVisible().catch(() => false))) return
  await page.getByTestId('nav-login-button').click()
  for (const candidate of candidates) {
    await page.getByTestId('login-account-input').fill(candidate)
    await page.getByTestId('login-password-input').fill('123456')
    await page.getByTestId('login-submit-button').click()
    const loggedIn = await page.waitForSelector('[data-testid="nav-login-button"]', {
      state: 'detached',
      timeout: 5000,
    }).then(() => true).catch(() => false)
    if (loggedIn) break
  }
  if (await page.getByTestId('nav-login-button').isVisible().catch(() => false)) {
    throw new Error(`UI login failed for ${account}`)
  }
  await waitForStable(page)
}

async function logoutIfAuthed(page) {
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  const userMenu = page.locator('.nav-user')
  if (await userMenu.isVisible().catch(() => false)) {
    await userMenu.hover()
    await page.getByRole('button', { name: '退出登录' }).click()
    await page.waitForSelector('[data-testid="nav-login-button"]', { timeout: 10000 })
  }
}

async function openUserFilter(page, keyword, statusLabel, roleLabel) {
  await page.goto(`${BASE_URL}/admin/users`, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  await page.getByPlaceholder('搜索用户名、邮箱或手机号').fill(keyword)
  if (statusLabel) {
    await page.locator('.filter-row .el-select').nth(0).click()
    await page.getByRole('option', { name: statusLabel }).click()
  }
  if (roleLabel) {
    await page.locator('.filter-row .el-select').nth(1).click()
    await page.getByRole('option', { name: roleLabel }).click()
  }
  await page.getByRole('button', { name: /^搜索$/ }).click()
  await waitForStable(page)
}

async function main() {
  const token = await getAdminToken()
  const enabledUser = await ensureUser(token, 'en', 1)
  const disabledUser = await ensureUser(token, 'dis', 0)
  const pendingUser = await ensureUser(token, 'pen', 2)
  const deleteUser = await ensureUser(token, 'del', 1)
  const resetApproveUser = await ensureUser(token, 'rapp', 1)
  const resetRejectUser = await ensureUser(token, 'rrej', 1)

  await submitReset(resetApproveUser.account)
  await submitReset(resetRejectUser.account)

  const publishedProblemId = await createProblem(token, `${PREFIX}_published_problem`, 'PUBLISHED')
  const draftProblemId = await createProblem(token, `${PREFIX}_draft_problem`, 'DRAFT')
  await createProblem(token, `${PREFIX}_archived_problem`, 'ARCHIVED')
  const editProblemId = await createProblem(token, `${PREFIX}_edit_problem`, 'DRAFT')
  const tag = await createTag(token, `${PREFIX}_tag`, '基础算法')
  const tagForDelete = await createTag(token, `${PREFIX}_delete_tag`, '临时')

  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 980 },
    deviceScaleFactor: 1,
  })
  const page = await context.newPage()

  await logoutIfAuthed(page)
  await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' })
  await page.getByText('请先登录').waitFor({ timeout: 10000 }).catch(() => {})
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-11-admin-guest-denied.png'), fullPage: false })

  await loginUi(page, 'user')
  await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' })
  await page.getByText('您没有权限访问此页面').waitFor({ timeout: 10000 }).catch(() => {})
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-12-admin-user-denied.png'), fullPage: false })

  await logoutIfAuthed(page)
  await loginUi(page, 'admin')
  await page.locator('.nav-user').hover()
  await page.getByText('管理员控制台').waitFor({ timeout: 10000 })
  await page.screenshot({ path: shot('admin-13-admin-menu-entry.png'), fullPage: false })
  await page.getByText('管理员控制台').click()
  await page.waitForURL(/\/admin$/)
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-14-admin-console-success.png'), fullPage: false })

  await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  await page.locator('.stat-card').first().screenshot({ path: shot('admin-15-overview-stat-card.png') }).catch(async () => {
    await page.screenshot({ path: shot('admin-15-overview-stat-card.png'), fullPage: false })
  })
  await page.getByText('用户状态统计').scrollIntoViewIfNeeded().catch(() => {})
  await page.screenshot({ path: shot('admin-16-overview-user-status-statistics.png'), fullPage: false })

  await openUserFilter(page, enabledUser.nickname, null, null)
  await page.screenshot({ path: shot('admin-17-users-keyword-search.png'), fullPage: false })
  await openUserFilter(page, disabledUser.nickname, '禁用', null)
  await page.screenshot({ path: shot('admin-18-users-status-filter-disabled.png'), fullPage: false })
  await openUserFilter(page, 'admin', null, '管理员')
  await page.screenshot({ path: shot('admin-19-users-role-filter-admin.png'), fullPage: false })

  await openUserFilter(page, enabledUser.nickname, null, null)
  await page.getByTestId(`view-user-${enabledUser.userId}`).click()
  await page.getByRole('dialog', { name: '用户详情' }).waitFor({ timeout: 10000 })
  await page.screenshot({ path: shot('admin-20-user-detail-dialog.png'), fullPage: false })
  await page.getByRole('button', { name: '关闭' }).click()

  await openUserFilter(page, pendingUser.nickname, null, null)
  await page.getByTestId(`set-status-enabled-${pendingUser.userId}`).click()
  await page.getByText('更新成功').waitFor({ timeout: 10000 }).catch(() => {})
  await page.screenshot({ path: shot('admin-21-user-status-enable-success.png'), fullPage: false })

  await openUserFilter(page, enabledUser.nickname, null, null)
  await page.getByTestId(`set-status-disabled-${enabledUser.userId}`).click()
  await page.getByText('更新成功').waitFor({ timeout: 10000 }).catch(() => {})
  await page.screenshot({ path: shot('admin-22-user-status-disable-success.png'), fullPage: false })

  await openUserFilter(page, disabledUser.nickname, null, null)
  await page.getByTestId(`set-status-pending-${disabledUser.userId}`).click()
  await page.getByText('更新成功').waitFor({ timeout: 10000 }).catch(() => {})
  await page.screenshot({ path: shot('admin-23-user-status-pending-success.png'), fullPage: false })

  await openUserFilter(page, deleteUser.nickname, null, null)
  await page.getByTestId(`delete-user-${deleteUser.userId}`).click()
  await page.getByText('确定删除该用户吗？').waitFor({ timeout: 10000 })
  await page.screenshot({ path: shot('admin-24-user-delete-confirm.png'), fullPage: false })
  await page.keyboard.press('Escape').catch(() => {})

  await openUserFilter(page, `${PREFIX}_r`, null, null)
  await page.screenshot({ path: shot('admin-25-password-reset-pending-list.png'), fullPage: false })
  const approveRow = page.locator('.el-table__row', { hasText: resetApproveUser.nickname }).first()
  await approveRow.getByRole('button', { name: '同意' }).click()
  await page.getByText('密码已重置为默认 123456').waitFor({ timeout: 10000 }).catch(() => {})
  await page.screenshot({ path: shot('admin-26-password-reset-approve-toast.png'), fullPage: false })
  const rejectRow = page.locator('.el-table__row', { hasText: resetRejectUser.nickname }).first()
  await rejectRow.getByRole('button', { name: '拒绝' }).click()
  await page.getByText('已拒绝重置申请').waitFor({ timeout: 10000 }).catch(() => {})
  await page.screenshot({ path: shot('admin-27-password-reset-reject-toast.png'), fullPage: false })

  await page.goto(`${BASE_URL}/admin/problems`, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  await page.getByPlaceholder('按标题搜索').fill(PREFIX)
  await page.getByRole('button', { name: /^搜索$/ }).click()
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-28-problems-published-search.png'), fullPage: false })
  await page.getByRole('tab', { name: '待审核（草稿）' }).click()
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-29-problems-draft-tab.png'), fullPage: false })
  await page.getByRole('tab', { name: '已归档' }).click()
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-30-problems-archived-tab.png'), fullPage: false })

  await page.getByTestId('create-problem-button').click()
  await page.waitForURL(/\/admin\/problems\/[^/]+$/, { timeout: 15000 })
  await page.getByText('题目草稿已创建').waitFor({ timeout: 10000 }).catch(() => {})
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-31-problem-create-draft-redirect.png'), fullPage: false })

  await page.goto(`${BASE_URL}/admin/problems`, { waitUntil: 'domcontentloaded' })
  await page.getByRole('tab', { name: '待审核（草稿）' }).click()
  await page.getByPlaceholder('按标题搜索').fill(`${PREFIX}_draft_problem`)
  await page.getByRole('button', { name: /^搜索$/ }).click()
  await waitForStable(page)
  await page.locator('.el-table__row', { hasText: `${PREFIX}_draft_problem` }).getByRole('button', { name: '发布' }).click()
  await page.getByText('发布成功').waitFor({ timeout: 10000 }).catch(() => {})
  await page.screenshot({ path: shot('admin-32-problem-publish-success.png'), fullPage: false })

  await page.goto(`${BASE_URL}/admin/problems`, { waitUntil: 'domcontentloaded' })
  await page.getByPlaceholder('按标题搜索').fill(`${PREFIX}_published_problem`)
  await page.getByRole('button', { name: /^搜索$/ }).click()
  await waitForStable(page)
  await page.locator('.el-table__row', { hasText: `${PREFIX}_published_problem` }).getByRole('button', { name: '归档' }).click()
  await page.getByText('归档成功').waitFor({ timeout: 10000 }).catch(() => {})
  await page.screenshot({ path: shot('admin-33-problem-archive-success.png'), fullPage: false })

  await page.goto(`${BASE_URL}/admin/problems/${editProblemId}`, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  await page.getByLabel('标题').fill(`${PREFIX}_edited_title`)
  await page.locator('.el-form-item', { hasText: '难度' }).locator('.el-select').click()
  await page.getByRole('option', { name: '中等' }).click()
  await page.getByLabel('时间限制（ms）').fill('1500')
  await page.getByLabel('内存限制（KB）').fill('131072')
  await page.screenshot({ path: shot('admin-34-problem-edit-basic-fields.png'), fullPage: false })

  await page.locator('.editor-pane textarea').fill('# doc_admin 题面\n\n- 支持 Markdown 编辑\n\n```text\n1 2\n```\n\n输出 `3`。')
  await page.screenshot({ path: shot('admin-35-problem-markdown-preview.png'), fullPage: false })

  await page.locator('.el-form-item', { hasText: '标签' }).locator('.el-select').click()
  await page.getByRole('option', { name: tag.name }).click()
  await page.keyboard.press('Escape')
  await page.screenshot({ path: shot('admin-36-problem-tag-bind.png'), fullPage: false })
  await page.locator('.el-form-item', { hasText: '标签' }).locator('.el-tag__close').first().click()
  await page.screenshot({ path: shot('admin-37-problem-tag-unbind.png'), fullPage: false })

  await page.locator('.case-block__title', { hasText: '样例测试用例' }).scrollIntoViewIfNeeded()
  await page.getByRole('button', { name: '新增样例' }).click()
  await page.locator('.case-block').first().locator('textarea').nth(0).fill('1 2')
  await page.locator('.case-block').first().locator('textarea').nth(1).fill('3')
  await page.screenshot({ path: shot('admin-38-problem-add-sample-case.png'), fullPage: false })
  await page.getByRole('button', { name: '新增隐藏用例' }).click()
  await page.locator('.case-block').nth(1).locator('textarea').nth(0).fill('10 20')
  await page.locator('.case-block').nth(1).locator('textarea').nth(1).fill('30')
  await page.screenshot({ path: shot('admin-39-problem-add-hidden-case.png'), fullPage: false })
  await page.locator('.case-block').first().getByRole('button', { name: '删除' }).first().click()
  await page.screenshot({ path: shot('admin-40-problem-delete-test-case.png'), fullPage: false })
  await page.getByRole('button', { name: '保存' }).click()
  await page.getByText('保存成功').waitFor({ timeout: 15000 }).catch(() => {})
  await page.screenshot({ path: shot('admin-41-problem-save-success.png'), fullPage: false })

  await page.goto(`${BASE_URL}/admin/tags`, { waitUntil: 'domcontentloaded' })
  await waitForStable(page)
  await page.locator('[data-testid="tag-name-input"] input').fill(`${PREFIX}_new_tag`)
  await page.locator('[data-testid="tag-type-input"] input').fill('动态规划')
  await page.screenshot({ path: shot('admin-42-tag-create-form.png'), fullPage: false })
  await page.getByTestId('tag-submit-button').click()
  await page.getByText('标签已创建').waitFor({ timeout: 10000 }).catch(() => {})
  await page.screenshot({ path: shot('admin-43-tag-create-result.png'), fullPage: false })
  await page.getByTestId(`edit-tag-${tag.id}`).click()
  await page.locator('[data-testid="tag-type-input"] input').fill('编辑后类型')
  await page.screenshot({ path: shot('admin-44-tag-edit-form.png'), fullPage: false })
  page.once('dialog', async (dialog) => {
    console.log(`tag delete confirm: ${dialog.message()}`)
    await dialog.dismiss()
  })
  await page.getByTestId(`delete-tag-${tagForDelete.id}`).click()
  await page.waitForTimeout(800)
  await page.screenshot({ path: shot('admin-45-tag-delete-confirm.png'), fullPage: false })
  await page.getByRole('button', { name: '刷新' }).click()
  await waitForStable(page)
  await page.screenshot({ path: shot('admin-46-tag-refresh-result.png'), fullPage: false })

  await browser.close()

  console.log(JSON.stringify({
    prefix: PREFIX,
    users: [enabledUser.nickname, disabledUser.nickname, pendingUser.nickname],
    publishedProblemId,
    draftProblemId,
    editProblemId,
    tag: tag.name,
  }, null, 2))
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
