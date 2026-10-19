import { defineConfig } from '@playwright/test';

// End-to-end tests drive the real app in Chrome.
// - Locally: against the dev server (`npm start` + backend on :8081) → http://localhost:4200
// - CI: against the packaged jar on a fresh database → E2E_BASE_URL=http://localhost:8081
export default defineConfig({
  testDir: './e2e',
  // Tests share one backend database: run them one at a time.
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    channel: 'chrome',
    viewport: { width: 1400, height: 1000 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
});
