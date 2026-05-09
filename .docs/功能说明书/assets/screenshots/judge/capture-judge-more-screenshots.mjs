import playwright from '../../../../../OJPT-frontend/node_modules/playwright/index.js'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const { chromium } = playwright

const baseUrl = process.env.OJPT_FRONTEND_URL || 'http://127.0.0.1:8110'
const apiUrl = process.env.OJPT_BACKEND_URL || 'http://127.0.0.1:8111'
const outDir = path.dirname(fileURLToPath(import.meta.url))
const executablePath = process.env.PLAYWRIGHT_EXECUTABLE_PATH || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'

const shot = async (target, name) => {
  await target.screenshot({
    path: path.join(outDir, name),
    animations: 'disabled',
  })
}

const login = async () => {
  const response = await fetch(`${apiUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ account: 'user', password: '123456' }),
  })
  if (!response.ok) throw new Error(`login failed: ${response.status}`)
  const body = await response.json()
  return body.data
}

const apiFetch = async (token, url, options = {}) => {
  const response = await fetch(`${apiUrl}${url}`, {
    ...options,
    headers: {
      authorization: `Bearer ${token}`,
      'content-type': 'application/json',
      ...(options.headers ?? {}),
    },
  })
  if (!response.ok) throw new Error(`${options.method ?? 'GET'} ${url} failed: ${response.status}`)
  return response.json()
}

const saveDraft = (token, language, sourceCode) =>
  apiFetch(token, '/api/problems/no/1/draft', {
    method: 'PUT',
    body: JSON.stringify({ language, sourceCode }),
  })

const cppAccepted = `#include <bits/stdc++.h>
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

const cppMessy = `#include <bits/stdc++.h>
using namespace std;
int main(){
int n; cin>>n;
vector<int> a(n);
for(int i=0;i<n;i++){cin>>a[i];}
int target; cin>>target;
cout << 0 << " " << 1 << "\\n";
return 0;
}
`

const javaDraft = `import java.util.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("java draft loaded");
    }
}
`

const pythonDraft = `# Python3 draft for screenshot
print("python draft loaded")
`

const pythonWrongAnswer = `import sys

def main():
    print("0 0")

if __name__ == "__main__":
    main()
`

const pythonRuntimeError = `import sys

raise RuntimeError("OJPT screenshot runtime error")
`

const cppCompileError = `#include <bits/stdc++.h>
using namespace std;

int main() {
    cout << "compile error"
    return 0;
}
`

const browser = await chromium.launch({ headless: true, executablePath })
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 })

const loginData = await login()
const { accessToken, refreshToken } = loginData

await saveDraft(accessToken, 'C/C++', cppAccepted)
await saveDraft(accessToken, 'Java', javaDraft)
await saveDraft(accessToken, 'Python3', pythonDraft)

await page.addInitScript(({ accessToken, refreshToken }) => {
  sessionStorage.setItem('ojpt_access_token', accessToken)
  localStorage.setItem('ojpt_refresh_token', refreshToken)
  localStorage.setItem('OJPT.solve.shortcutTipsHidden', 'true')
  localStorage.setItem('OJPT.solve.leftSplitRatio', '0.43')
  localStorage.setItem('OJPT.solve.editorSplitRatio', '0.56')
}, { accessToken, refreshToken })

const gotoProblem = async () => {
  await page.goto(`${baseUrl}/problems/1`, { waitUntil: 'networkidle' })
  await page.locator('textarea.code-editor').waitFor({ timeout: 20000 })
}

const editor = () => page.locator('textarea.code-editor')
const editorTop = () => page.locator('.editor-card--top')
const editorBottom = () => page.locator('.editor-card--bottom')

const closeRunDialog = async () => {
  const close = page.locator('[data-testid="run-result-dialog"] .submit-dialog__close')
  await close.click()
  await page.locator('[data-testid="run-result-dialog"]').waitFor({ state: 'detached', timeout: 10000 })
}

const runAndCapture = async (name) => {
  await page.getByTestId('run-code-button').click()
  await page.getByTestId('run-result-dialog').waitFor({ timeout: 10000 })
  await page.getByTestId('run-case-result').first().waitFor({ timeout: 60000 })
  await shot(page.getByTestId('run-result-dialog'), name)
  await closeRunDialog()
}

try {
  await page.goto(`${baseUrl}/problemset`, { waitUntil: 'networkidle' })
  await page.getByTestId('problem-search-input').fill('P0001')
  await page.waitForTimeout(800)
  await shot(page, 'judge-10-problemset-before-click.png')
  await page.locator('a.problem-link').first().click()
  await page.waitForURL(/\/problems\/1/, { timeout: 10000 })
  await page.locator('textarea.code-editor').waitFor({ timeout: 20000 })
  await shot(page, 'judge-11-problem-opened-from-problemset.png')

  await shot(editorTop(), 'judge-12-editor-language-cpp-draft.png')
  await page.locator('select.language-select').selectOption('Java')
  await page.waitForTimeout(900)
  await shot(editorTop(), 'judge-13-editor-language-java-draft.png')
  await page.locator('select.language-select').selectOption('Python3')
  await page.waitForTimeout(900)
  await shot(editorTop(), 'judge-14-editor-language-python-draft.png')

  await page.getByTestId('reset-code-button').click()
  await page.locator('.el-message-box').waitFor({ timeout: 10000 })
  await shot(page.locator('.el-overlay-message-box'), 'judge-15-reset-code-confirm.png')
  await page.locator('.el-message-box .el-button--primary').click()
  await page.waitForTimeout(500)

  await editor().fill(`${pythonDraft}\nprint("unsaved change before switch")\n`)
  await page.locator('select.language-select').selectOption('C/C++')
  await page.locator('.el-message-box').waitFor({ timeout: 10000 })
  await shot(page.locator('.el-overlay-message-box'), 'judge-16-language-switch-unsaved-confirm.png')
  await page.locator('.el-message-box .el-button--primary').click()
  await page.waitForTimeout(500)

  await editor().fill(cppMessy)
  await shot(editorTop(), 'judge-17-format-before.png')
  await page.getByTestId('format-code-button').click()
  await page.waitForTimeout(500)
  await shot(editorTop(), 'judge-18-format-after.png')

  await editor().focus()
  await page.keyboard.press('Control+A')
  await page.getByTestId('comment-code-button').click()
  await page.waitForTimeout(500)
  await shot(editorTop(), 'judge-19-comment-code.png')
  await page.getByTestId('comment-code-button').click()
  await page.waitForTimeout(500)
  await shot(editorTop(), 'judge-20-uncomment-code.png')

  await editor().focus()
  await page.keyboard.press('Control+A')
  await page.keyboard.press('Tab')
  await page.waitForTimeout(500)
  await shot(editorTop(), 'judge-21-indent-code.png')
  await page.keyboard.press('Shift+Tab')
  await page.waitForTimeout(500)
  await shot(editorTop(), 'judge-22-unindent-code.png')

  await page.getByTestId('shortcut-help-button').click()
  await page.getByTestId('shortcut-tips-dialog').waitFor({ timeout: 10000 })
  await shot(page.getByTestId('shortcut-tips-dialog'), 'judge-23-shortcut-help-dialog.png')
  await page.getByTestId('shortcut-tips-close').click()

  await gotoProblem()
  await shot(editorTop(), 'judge-24-draft-auto-loaded-cpp.png')
  await editor().fill(`${cppAccepted}\n// manual save by Ctrl+S\n`)
  await editor().focus()
  await page.keyboard.press('Control+S')
  await page.waitForTimeout(1200)
  await shot(editorTop(), 'judge-25-draft-manual-save-ctrl-s.png')
  await page.locator('select.language-select').selectOption('Python3')
  await page.waitForTimeout(900)
  await shot(editorTop(), 'judge-26-draft-language-specific-python.png')
  await page.locator('select.language-select').selectOption('C/C++')
  await page.waitForTimeout(900)
  await shot(editorTop(), 'judge-27-draft-language-specific-cpp.png')

  await shot(editorBottom(), 'judge-28-testcase-initial-tabs.png')
  await page.locator('.testcase-tab--add').click()
  await page.waitForTimeout(400)
  await shot(editorBottom(), 'judge-29-testcase-add-custom.png')
  const textareas = page.locator('.editor-card--bottom textarea.testcase-textarea')
  await textareas.nth(0).fill('4\n1 5 8 9\n10')
  await textareas.nth(1).fill('0 3')
  await shot(editorBottom(), 'judge-30-testcase-edit-input-output.png')
  await page.locator('.testcase-tab--add').click()
  await page.waitForTimeout(400)
  await shot(editorBottom(), 'judge-31-testcase-copy-current-by-add.png')
  await page.locator('.testcase-tab').nth(1).click()
  await page.waitForTimeout(300)
  await shot(editorBottom(), 'judge-32-testcase-switch-case-tab.png')
  await page.locator('.testcase-tab:not(.testcase-tab--add)').last().hover()
  await page.locator('.testcase-tab:not(.testcase-tab--add)').last().locator('.testcase-delete-btn').click()
  await page.waitForTimeout(400)
  await shot(editorBottom(), 'judge-33-testcase-delete-case.png')

  await page.locator('select.language-select').selectOption('C/C++')
  await page.waitForTimeout(900)
  await editor().fill(cppAccepted)
  await runAndCapture('judge-34-run-accepted.png')

  await page.locator('select.language-select').selectOption('Python3')
  await page.waitForTimeout(900)
  await editor().fill(pythonWrongAnswer)
  await runAndCapture('judge-35-run-wrong-answer.png')

  await page.locator('select.language-select').selectOption('C/C++')
  await page.waitForTimeout(900)
  await editor().fill(cppCompileError)
  await runAndCapture('judge-36-run-compile-error.png')

  await page.locator('select.language-select').selectOption('Python3')
  await page.waitForTimeout(900)
  await editor().fill(pythonRuntimeError)
  await runAndCapture('judge-37-run-runtime-error-stderr.png')

  await page.locator('select.language-select').selectOption('C/C++')
  await page.waitForTimeout(900)
  await editor().fill(cppAccepted)
  await page.getByTestId('submit-code-button').click()
  await page.locator('.submit-dialog[aria-label]').waitFor({ timeout: 10000 })
  await shot(page.locator('.submit-dialog[aria-label]').last(), 'judge-38-submit-click-polling.png')

  await gotoProblem()
  let acPoll = 0
  await page.route('**/api/problems/no/1/submissions', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        message: 'success',
        data: {
          submissionId: 'doc-ac',
          status: 'QUEUED',
          message: 'queued',
          totalCaseCount: 3,
          caseResults: [],
        },
        timestamp: Date.now(),
      }),
    })
  })
  await page.route('**/api/problems/submissions/doc-ac', async (route) => {
    acPoll += 1
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        message: 'success',
        data: acPoll < 2
          ? { submissionId: 'doc-ac', status: 'RUNNING', message: 'judging', totalCaseCount: 3, caseResults: [] }
          : {
              submissionId: 'doc-ac',
              status: 'AC',
              message: 'Accepted',
              timeMs: 12,
              totalCaseCount: 3,
              rank: 1,
              rankStats: {
                acceptedCount: 2,
                timeBuckets: [{ label: '12 ms', min: 12, max: 12, count: 1 }],
              },
              caseResults: [
                { caseIndex: 0, caseType: 'SAMPLE', status: 'AC', inputText: '4\\n2 7 11 15\\n9', expectedOutput: '0 1', actualOutput: '0 1', errorOutput: '', timeMs: 8, message: 'Accepted' },
                { caseIndex: 1, caseType: 'HIDDEN', status: 'AC', inputText: null, expectedOutput: null, actualOutput: null, errorOutput: null, timeMs: 12, message: 'Accepted' },
                { caseIndex: 2, caseType: 'HIDDEN', status: 'AC', inputText: null, expectedOutput: null, actualOutput: null, errorOutput: null, timeMs: 11, message: 'Accepted' },
              ],
            },
        timestamp: Date.now(),
      }),
    })
  })
  await editor().fill(cppAccepted)
  await page.getByTestId('submit-code-button').click()
  await page.locator('.submit-summary--ac').waitFor({ timeout: 10000 })
  await shot(page.locator('.submit-dialog[aria-label]').last(), 'judge-39-submit-ac-detail.png')
  await page.unroute('**/api/problems/no/1/submissions')
  await page.unroute('**/api/problems/submissions/doc-ac')

  await gotoProblem()
  await page.route('**/api/problems/no/1/submissions', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        message: 'success',
        data: { submissionId: 'doc-wa-hidden', status: 'QUEUED', message: 'queued', totalCaseCount: 3, caseResults: [] },
        timestamp: Date.now(),
      }),
    })
  })
  await page.route('**/api/problems/submissions/doc-wa-hidden', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        message: 'success',
        data: {
          submissionId: 'doc-wa-hidden',
          status: 'WA',
          message: 'Wrong Answer on hidden case',
          timeMs: 18,
          totalCaseCount: 3,
          rank: null,
          rankStats: { acceptedCount: 2, timeBuckets: [{ label: '12 ms', min: 12, max: 12, count: 1 }] },
          caseResults: [
            { caseIndex: 0, caseType: 'SAMPLE', status: 'AC', inputText: '4\\n2 7 11 15\\n9', expectedOutput: '0 1', actualOutput: '0 1', errorOutput: '', timeMs: 9, message: 'Accepted' },
            { caseIndex: 1, caseType: 'HIDDEN', status: 'WA', inputText: 'redacted hidden input', expectedOutput: 'redacted expected', actualOutput: 'redacted actual', errorOutput: 'redacted stderr', timeMs: 18, message: 'Wrong Answer' },
          ],
        },
        timestamp: Date.now(),
      }),
    })
  })
  await editor().fill(cppAccepted.replace("cout << seen[need] << ' ' << i << '\\n';", "cout << 0 << ' ' << 0 << '\\n';"))
  await page.getByTestId('submit-code-button').click()
  await page.getByTestId('submit-case-result').waitFor({ timeout: 10000 })
  await shot(page.locator('.submit-dialog[aria-label]').last(), 'judge-40-submit-failed-hidden-redacted.png')
} finally {
  await browser.close()
}
