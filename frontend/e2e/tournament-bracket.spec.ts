import { expect, Page, test } from '@playwright/test';
import { TournamentApi } from './support/tournament-api';
import { submitScore } from './support/score-form';

test.describe('Tournament bracket', () => {
  let api: TournamentApi;
  let tournamentId: string;

  test.beforeEach(async ({ request }) => {
    api = new TournamentApi(request);
    tournamentId = await api.createStartedTournament('E2E Bracket');
  });

  test.afterEach(async () => {
    await api.deleteTournament(tournamentId);
  });

  async function generateBracket(page: Page, totalQualifiedTeams: number): Promise<void> {
    await page.getByRole('button', { name: 'Générer le bracket' }).click();
    await page.locator('input[formControlName=totalQualifiedTeams]').fill(String(totalQualifiedTeams));
    await page.locator('mat-dialog-container').getByRole('button', { name: 'Générer' }).click();
  }

  async function playMatch(page: Page, row: ReturnType<Page['locator']>, score1: number, score2: number): Promise<void> {
    await row.click();
    await submitScore(page, score1, score2);
    await expect(row.locator('td').last()).toHaveText(`${score1} - ${score2}`);
  }

  test('should show the API error inside the generate modal', async ({ page }) => {
    await api.enterAllGroupResults(tournamentId);
    await page.goto(`/${tournamentId}/bracket`);

    await generateBracket(page, 6);

    await expect(page.locator('mat-dialog-container .error-text')).toContainText('puissance de 2');
  });

  test('should warn while group results are missing', async ({ page }) => {
    await page.goto(`/${tournamentId}/bracket`);

    await expect(page.locator('.warning-banner')).toBeVisible();
  });

  test('should play the bracket through to a finished tournament', async ({ page }) => {
    await api.enterAllGroupResults(tournamentId);
    await page.goto(`/${tournamentId}/bracket`);
    await expect(page.locator('.warning-banner')).toHaveCount(0);

    await generateBracket(page, 4);

    await expect(page.locator('mat-dialog-container')).toHaveCount(0);
    await expect(page.locator('.round-name')).toHaveText(['Demi-finales', 'Finale', 'Troisième place']);
    await expect(page.locator('.round-time').first()).toHaveText('14:00');
    await expect(page.getByRole('button', { name: 'Terminer le tournoi' })).toHaveCount(0);

    const round = (name: string) => page.locator('.round-section', { has: page.locator('.round-name', { hasText: new RegExp(`^${name}$`) }) });
    await expect(round('Finale').locator('tbody tr')).toContainText('À déterminer');

    await playMatch(page, round('Demi-finales').locator('tbody tr').nth(0), 2, 1);
    await playMatch(page, round('Demi-finales').locator('tbody tr').nth(1), 3, 0);

    const finalRow = round('Finale').locator('tbody tr');
    const thirdPlaceRow = round('Troisième place').locator('tbody tr');
    await expect(finalRow).toHaveClass(/playable/);
    await expect(thirdPlaceRow).toHaveClass(/playable/);

    await playMatch(page, thirdPlaceRow, 1, 0);
    await expect(page.getByRole('button', { name: 'Terminer le tournoi' })).toHaveCount(0);
    await playMatch(page, finalRow, 4, 2);

    await page.getByRole('button', { name: 'Terminer le tournoi' }).click();
    await page.locator('mat-dialog-container').getByRole('button', { name: 'Terminer' }).click();

    await expect(page.locator('.detail-panel')).toContainText('Le tournoi est terminé');
    await expect(page.locator('.bracket-table tr.playable')).toHaveCount(0);

    await page.goto(`/${tournamentId}/results`);
    await page.locator('.match-row').first().click();
    await expect(page.locator('.result-form')).toContainText('Tournoi terminé');
    await expect(page.locator('app-score-form')).toHaveCount(0);
  });
});
