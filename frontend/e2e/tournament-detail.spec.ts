import { expect, test } from '@playwright/test';
import { TournamentApi } from './support/tournament-api';

test.describe('Tournament detail (teams)', () => {
  let api: TournamentApi;
  let tournamentId: string;

  test.beforeEach(async ({ request }) => {
    api = new TournamentApi(request);
    tournamentId = await api.createTournament('E2E Équipes');
    await api.registerTeams(tournamentId);
  });

  test.afterEach(async () => {
    await api.deleteTournament(tournamentId);
  });

  test('should list teams by organisation with edit actions while DRAFT', async ({ page }) => {
    await page.goto(`/${tournamentId}`);

    await expect(page.locator('.org-header')).toHaveCount(4);
    await expect(page.locator('.team-row:not(.team-header-row)')).toHaveCount(8);
    await expect(page.locator('.team-row mat-icon', { hasText: 'edit' })).toHaveCount(8);
  });

  test('should toggle a team as paid', async ({ page }) => {
    await page.goto(`/${tournamentId}`);
    await expect(page.locator('button[title="Marquer payé"]')).toHaveCount(8);

    await page.locator('button[title="Marquer payé"]').first().click();

    await expect(page.locator('button[title="Marquer payé"]')).toHaveCount(7);
    await expect(page.locator('button[title="Marquer non payé"]')).toHaveCount(1);
  });

  test('should hide edit actions once the tournament is started', async ({ page }) => {
    await api.generateGroups(tournamentId);
    await api.generateSchedule(tournamentId);
    await api.start(tournamentId);

    await page.goto(`/${tournamentId}`);

    await expect(page.locator('.org-header')).toHaveCount(4);
    await expect(page.locator('.team-row mat-icon', { hasText: 'edit' })).toHaveCount(0);
  });
});
