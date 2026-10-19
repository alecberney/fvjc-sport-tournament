import { expect, test } from '@playwright/test';
import { TournamentApi } from './support/tournament-api';

test.describe('Tournament list', () => {
  let api: TournamentApi;
  let tournamentId: string;

  test.beforeEach(async ({ request }) => {
    api = new TournamentApi(request);
    tournamentId = await api.createTournament('E2E Liste');
  });

  test.afterEach(async () => {
    await api.deleteTournament(tournamentId);
  });

  test('should show the tournament card with formatted labels', async ({ page }) => {
    await page.goto('/');

    const card = page.locator('mat-card', { hasText: 'E2E Liste' });
    await expect(card).toContainText('15 Aug 2027');
    await expect(card).toContainText('2 terrain(s)');
    await expect(card).toContainText('2–4 joueurs/équipe');
    await expect(card).toContainText('Brouillon');
  });

  test('should open the tournament when clicking its card', async ({ page }) => {
    await page.goto('/');

    await page.locator('mat-card', { hasText: 'E2E Liste' }).click();

    await expect(page).toHaveURL(new RegExp(`/${tournamentId}$`));
  });
});
