import { APIRequestContext, expect } from '@playwright/test';

// Builds test data through the REST API (same /api the app uses), so each spec starts
// from a known tournament state and only drives the UI for what it actually tests.
export class TournamentApi {

  constructor(private readonly request: APIRequestContext) {}

  async createTournament(name: string): Promise<string> {
    const response = await this.request.post('/api/tournaments', {
      data: {
        name,
        sport: 'FOOTBALL',
        numberOfFields: 2,
        minPlayersPerTeam: 2,
        maxPlayersPerTeam: 4,
        date: '2027-08-15',
      },
    });
    expect(response.status()).toBe(201);
    return (await response.json()).id;
  }

  // 4 organisations × 2 unpaid teams = 8 teams.
  async registerTeams(tournamentId: string): Promise<void> {
    for (let i = 0; i < 4; i++) {
      const response = await this.request.post(`/api/tournaments/${tournamentId}/teams`, {
        data: { name: `Club${i}`, responsibleFirstName: `Resp${i}`, responsibleLastName: 'Test', count: 2, paid: [false, false] },
      });
      expect(response.ok()).toBe(true);
    }
  }

  // 2 groups of 4 teams.
  async generateGroups(tournamentId: string): Promise<void> {
    const response = await this.request.post(`/api/tournaments/${tournamentId}/groups/generate`, { data: { groupSize: 4 } });
    expect(response.ok()).toBe(true);
  }

  async generateSchedule(tournamentId: string): Promise<void> {
    const response = await this.request.post(`/api/tournaments/${tournamentId}/schedule/generate`, {
      data: { startTime: '09:00', matchDurationMinutes: 15, breakDurationMinutes: 5 },
    });
    expect(response.ok()).toBe(true);
  }

  async start(tournamentId: string): Promise<void> {
    const response = await this.request.post(`/api/tournaments/${tournamentId}/start`);
    expect(response.ok()).toBe(true);
  }

  // Teams registered, groups and schedule generated, tournament IN_PROGRESS.
  async createStartedTournament(name: string): Promise<string> {
    const tournamentId = await this.createTournament(name);
    await this.registerTeams(tournamentId);
    await this.generateGroups(tournamentId);
    await this.generateSchedule(tournamentId);
    await this.start(tournamentId);
    return tournamentId;
  }

  // Enters a deterministic, draw-free score for every group match still without a result.
  async enterAllGroupResults(tournamentId: string): Promise<void> {
    const schedule = await (await this.request.get(`/api/tournaments/${tournamentId}/schedule`)).json();
    const matches = schedule.rounds.flatMap((round: { matches: unknown[] }) => round.matches);
    for (const [index, match] of matches.entries()) {
      if (match.result) continue;
      const response = await this.request.put(`/api/tournaments/${tournamentId}/matches/${match.id}/result`, {
        data: { score1: index % 5, score2: 5 + (index % 3) },
      });
      expect(response.ok()).toBe(true);
    }
  }

  async deleteTournament(tournamentId: string): Promise<void> {
    await this.request.delete(`/api/tournaments/${tournamentId}`);
  }
}
