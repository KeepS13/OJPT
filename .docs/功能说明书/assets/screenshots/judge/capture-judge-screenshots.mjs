import playwright from '../../../../../OJPT-frontend/node_modules/playwright/index.js'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const { chromium } = playwright

const baseUrl = process.env.OJPT_FRONTEND_URL || 'http://127.0.0.1:8110'
const apiUrl = process.env.OJPT_BACKEND_URL || 'http://127.0.0.1:8111'
const outDir = path.dirname(fileURLToPath(import.meta.url))

const shot = async (pageOrLocator, name) => {
  await pageOrLocator.screenshot({
    path: path.join(outDir, name),
    animations: 'disabled',
  })
}

const waitForText = async (page, text, timeout = 15000) => {
  await page.getByText(text, { exact: false }).first().waitFor({ timeout })
}

const cppTwoSum = `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int n;
    if (!(cin >> n)) return 0;
    vector<long long> nums(n);
    for (int i = 0; i < n; ++i) cin >> nums[i];
    long long target;
    cin >> target;

    unordered_map<long long, int> seen;
    for (int i = 0; i < n; ++i) {
        long long need = target - nums[i];
        if (seen.count(need)) {
            cout << seen[need] << ' ' << i << '\\n';
            return 0;
        }
        seen[nums[i]] = i;
    }
    return 0;
}
`

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 })

try {
  const loginResponse = await fetch(`${apiUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ account: 'user', password: '123456' }),
  })
  const loginBody = await loginResponse.json()
  const { accessToken, refreshToken } = loginBody.data
  await page.addInitScript(({ accessToken, refreshToken }) => {
    sessionStorage.setItem('ojpt_access_token', accessToken)
    localStorage.setItem('ojpt_refresh_token', refreshToken)
    localStorage.setItem('OJPT.solve.shortcutTipsHidden', 'true')
  }, { accessToken, refreshToken })

  await page.goto(`${baseUrl}/problemset`, { waitUntil: 'networkidle' })
  await page.getByTestId('problem-search-input').fill('P0001')
  await page.waitForTimeout(1000)
  await shot(page, 'judge-01-problemset-search.png')

  await page.goto(`${baseUrl}/problems/1`, { waitUntil: 'networkidle' })
  await page.locator('textarea.code-editor').waitFor({ timeout: 20000 })
  await waitForText(page, '题目描述')
  await shot(page.locator('.statement-panel'), 'judge-02-problem-statement.png')

  const editor = page.locator('textarea.code-editor')
  await editor.fill(cppTwoSum)
  await shot(page.locator('.editor-card--top'), 'judge-03-code-editor.png')

  await shot(page.locator('.editor-card--bottom'), 'judge-04-sample-testcases.png')

  await editor.focus()
  await page.keyboard.press('Control+S')
  await waitForText(page, '已同步', 30000)
  await shot(page.locator('.editor-card--top'), 'judge-05-draft-saved.png')

  await page.getByTestId('run-code-button').click()
  await page.getByTestId('run-result-dialog').waitFor({ timeout: 10000 })
  await shot(page.locator('[data-testid="run-result-dialog"]'), 'judge-06-run-pending.png')
  await page.getByTestId('run-case-result').first().waitFor({ timeout: 60000 })
  await shot(page.locator('[data-testid="run-result-dialog"]'), 'judge-07-run-result.png')
  await page.getByLabel('关闭运行详情').click()

  await page.getByTestId('submit-code-button').click()
  await page.getByRole('dialog', { name: '提交详情' }).waitFor({ timeout: 10000 })
  await shot(page.getByRole('dialog', { name: '提交详情' }), 'judge-08-submit-judging.png')
  try {
    await page.locator('.submit-summary').first().waitFor({ timeout: 70000 })
    await shot(page.getByRole('dialog', { name: '提交详情' }), 'judge-09-submit-result.png')
  } catch {
    await page.goto(`${baseUrl}/profile/submissions`, { waitUntil: 'networkidle' })
    await page.getByText('AC', { exact: true }).first().waitFor({ timeout: 20000 })
    await shot(page, 'judge-09-submit-result.png')
  }
} finally {
  await browser.close()
}
