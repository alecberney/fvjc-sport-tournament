import { expect, test } from '@playwright/test';
import { TournamentApi } from './support/tournament-api';
import { submitScore } from './support/score-form';

test.describe('Tournament results', () => {
  let api: TournamentApi;
  let tournamentId: string;

  test.beforeEach(async ({ request }) => {
    api = new TournamentApi(request);
    tournamentId = await api.createStartedTournament('E2E Résultats');
  });

  test.afterEach(async () => {
    await api.deleteTournament(tournamentId);
  });

  test('should enter a result and show it with the group ranking', async ({ page }) => {
    await page.goto(`/${tournamentId}/results`);
    await expect(page.locator('.empty-detail')).toContainText('Sélectionnez un match');
    const firstRow = page.locator('.match-row').first();

    await firstRow.click();
    await expect(firstRow).toHaveClass(/selected/);
    await expect(page.locator('.result-form h2')).toContainText(' vs ');
    await expect(page.locator('.ranking-panel h3')).toContainText('Classement — Groupe');
    await submitScore(page, 3, 1);

    await expect(firstRow.locator('.match-score')).toHaveText('3 - 1');
    await expect(firstRow).toHaveClass(/has-result/);
  });

  test('should reset the form per match and re-seed saved scores', async ({ page }) => {
    await page.goto(`/${tournamentId}/results`);
    const rows = page.locator('.match-row');
    const inputs = page.locator('app-score-form input');

    await rows.nth(0).click();
    await submitScore(page, 4, 2);
    await expect(rows.nth(0).locator('.match-score')).toHaveText('4 - 2');

    await rows.nth(1).click();
    await expect(inputs.nth(0)).toHaveValue('');
    await expect(inputs.nth(1)).toHaveValue('');

    await rows.nth(0).click();
    await expect(inputs.nth(0)).toHaveValue('4');
    await expect(inputs.nth(1)).toHaveValue('2');
  });
});
