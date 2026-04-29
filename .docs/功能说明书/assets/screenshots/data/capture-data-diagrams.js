const path = require("path");
const { chromium } = require("../../../../../OJPT-frontend/node_modules/playwright");

const baseDir = __dirname;
const htmlPath = path.join(baseDir, "data-model-diagrams.html");
const targets = [
  ["user-model", "data-01-user-profile-role-model.png"],
  ["problem-model", "data-02-problem-bank-model.png"],
  ["submission-model", "data-03-submission-progress-model.png"],
  ["state-flow", "data-04-business-state-flow.png"],
  ["table-list", "data-05-core-table-list.png"],
];

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  await page.goto(`file://${htmlPath.replace(/\\/g, "/")}`);
  await page.emulateMedia({ media: "screen" });

  for (const [id, fileName] of targets) {
    const locator = page.locator(`#${id}`);
    await locator.screenshot({ path: path.join(baseDir, fileName) });
  }

  await browser.close();
})();
