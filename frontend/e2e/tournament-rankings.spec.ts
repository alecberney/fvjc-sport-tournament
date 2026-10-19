import { expect, test } from '@playwright/test';
import { TournamentApi } from './support/tournament-api';

test.describe('Tournament rankings', () => {
  let api: TournamentApi;
  let tournamentId: string;

  test.beforeEach(async ({ request }) => {
    api = new TournamentApi(request);
    tournamentId = await api.createStartedTournament('E2E Classements');
    await api.enterAllGroupResults(tournamentId);
  });

  test.afterEach(async () => {
    await api.deleteTournament(tournamentId);
  });

  test('should filter the rankings by group', async ({ page }) => {
    await page.goto(`/${tournamentId}/rankings`);
    await expect(page.locator('.group-ranking')).toHaveCount(2);

    await page.locator('mat-select').click();
    await page.getByRole('option', { name: 'Groupe A' }).click();

    await expect(page.locator('.group-ranking h2')).toHaveText(['Groupe A']);
    await expect(page.locator('mat-select')).toContainText('Groupe A');

    await page.locator('mat-select').click();
    await page.getByRole('option', { name: 'Tous les groupes' }).click();

    await expect(page.locator('.group-ranking')).toHaveCount(2);
  });
});
