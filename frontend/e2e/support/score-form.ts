import { Page } from '@playwright/test';

export async function submitScore(page: Page, score1: number, score2: number): Promise<void> {
  const inputs = page.locator('app-score-form input');
  await inputs.nth(0).fill(String(score1));
  await inputs.nth(1).fill(String(score2));
  await page.locator('app-score-form button[type=submit]').click();
}
