const path = require("path");
const { chromium } = require("../../../../../OJPT-frontend/node_modules/playwright");

const baseDir = __dirname;
const htmlPath = path.join(baseDir, "paper-engineering-figures.html");

const targetIds = [
  "fig-3-1-function-modules",
  "fig-3-2-top-dfd",
  "fig-3-3-zero-dfd",
  "fig-3-4-level1-judge-dfd",
  "fig-4-1-architecture",
  "fig-4-2-deployment",
  "fig-4-3-user-entity",
  "fig-4-4-problem-entity",
  "fig-4-5-tag-entity",
  "fig-4-6-testcase-entity",
  "fig-4-7-code-draft-entity",
  "fig-4-8-submission-entity",
  "fig-4-9-submission-case-result-entity",
  "fig-4-10-user-problem-progress-entity",
  "fig-4-11-er-overview",
  "fig-4-12-jwt-auth-flow",
  "fig-5-1-login-flow",
  "fig-5-2-problem-filter-flow",
  "fig-5-3-code-draft-flow",
  "fig-5-4-code-run-flow",
  "fig-5-5-submit-async-judge-flow",
  "fig-5-6-admin-problem-flow",
  "fig-5-7-register-flow",
  "fig-5-8-password-reset-request-flow",
  "fig-5-9-logout-flow",
  "fig-5-10-session-restore-flow",
  "fig-5-11-profile-load-flow",
  "fig-5-12-profile-save-flow",
  "fig-5-13-avatar-upload-flow",
  "fig-5-14-avatar-delete-flow",
  "fig-5-15-security-update-flow",
  "fig-5-16-password-update-flow",
  "fig-5-17-account-delete-flow",
  "fig-5-18-user-submissions-flow",
  "fig-5-19-training-dashboard-flow",
  "fig-5-20-problem-detail-init-flow",
  "fig-5-21-sample-testcase-flow",
  "fig-5-22-code-draft-load-flow",
  "fig-5-23-submission-poll-flow",
  "fig-5-24-submission-detail-flow",
  "fig-5-25-judge-dispatch-flow",
  "fig-5-26-judge-internal-flow",
  "fig-5-27-docker-execution-flow",
  "fig-5-28-case-result-flow",
  "fig-5-29-progress-update-flow",
  "fig-5-30-admin-overview-flow",
  "fig-5-31-admin-judge-health-flow",
  "fig-5-32-admin-user-list-flow",
  "fig-5-33-admin-user-detail-flow",
  "fig-5-34-admin-user-edit-flow",
  "fig-5-35-admin-user-status-flow",
  "fig-5-36-admin-user-delete-flow",
  "fig-5-37-admin-password-reset-review-flow",
  "fig-5-38-admin-problem-list-flow",
  "fig-5-39-admin-problem-create-flow",
  "fig-5-40-admin-problem-edit-load-flow",
  "fig-5-41-admin-problem-save-flow",
  "fig-5-42-admin-testcase-maintain-flow",
  "fig-5-43-admin-problem-publish-archive-flow",
  "fig-5-44-admin-tag-list-flow",
  "fig-5-45-admin-tag-save-flow",
  "fig-5-46-admin-tag-delete-flow",
  "fig-5-47-admin-problem-tag-bind-flow",
  "fig-5-48-admin-navigation-flow",
];

const targets = targetIds.map((id) => [id, `${id}.png`]);

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({
    viewport: { width: 1480, height: 1100 },
    deviceScaleFactor: 1,
  });

  await page.goto(`file://${htmlPath.replace(/\\/g, "/")}`);
  await page.emulateMedia({ media: "screen" });

  for (const [id, fileName] of targets) {
    const locator = page.locator(`#${id}`);
    await locator.screenshot({ path: path.join(baseDir, fileName) });
  }

  await browser.close();
})();
