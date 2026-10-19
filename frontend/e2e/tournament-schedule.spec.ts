import { expect, test } from '@playwright/test';
import { TournamentApi } from './support/tournament-api';

test.describe('Tournament schedule', () => {
  let api: TournamentApi;
  let tournamentId: string;

  test.beforeEach(async ({ request }) => {
    api = new TournamentApi(request);
    tournamentId = await api.createTournament('E2E Horaire');
    await api.registerTeams(tournamentId);
    await api.generateGroups(tournamentId);
    await api.generateSchedule(tournamentId);
  });

  test.afterEach(async () => {
    await api.deleteTournament(tournamentId);
  });

  test('should show the rounds and start the tournament', async ({ page }) => {
    await page.goto(`/${tournamentId}/schedule`);

    await expect(page.locator('.round-number').first()).toHaveText('Round 1');
    await expect(page.locator('.round-time').first()).toHaveText('09:00');

    await page.getByRole('button', { name: 'Démarrer le tournoi' }).click();
    await page.locator('mat-dialog-container').getByRole('button', { name: 'Démarrer' }).click();

    await expect(page).toHaveURL(new RegExp(`/${tournamentId}/results$`));
  });
});
