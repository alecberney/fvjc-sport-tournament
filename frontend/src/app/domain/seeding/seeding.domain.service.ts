import { Injectable } from '@angular/core';
import { TeamRegistration } from '@app/domain/team/team.model';
import { Schedule } from '@app/domain/schedule/schedule.model';
import { BracketRound } from '@app/domain/bracket/bracket.model';
import { RandomMatchResult } from '@app/domain/seeding/seeding.model';

// Random test data used by the settings cog ("Générer des équipes aléatoires", "Résultats aléatoires …").

const RANDOM_TEAM_NAMES = [
  'Les Aigles', 'Les Loups', 'Les Renards', 'Les Ours', 'Les Lynx', 'Les Faucons',
  'Les Bouquetins', 'Les Chamois', 'Les Sangliers', 'Les Hiboux', 'Les Castors', 'Les Marmottes',
];
const RANDOM_FIRST_NAMES = ['Jean', 'Marie', 'Luc', 'Sophie', 'Pierre', 'Julie', 'Marc', 'Laura', 'Nicolas', 'Camille'];
const RANDOM_LAST_NAMES = ['Dupont', 'Martin', 'Favre', 'Rochat', 'Bovet', 'Golay', 'Cuendet', 'Pittet', 'Chappuis', 'Mermoud'];
const MAX_TEAMS_PER_ORGANISATION = 3;
const MAX_RANDOM_SCORE = 5;

@Injectable({ providedIn: 'root' })
export class SeedingDomainService {

  /**
   * Builds registrations totalling exactly `count` teams, for test data.
   * Teams are split into organisations of 1 to MAX_TEAMS_PER_ORGANISATION teams, each with a
   * random responsible and random paid flags. Base names cycle through RANDOM_TEAM_NAMES and get
   * a numeric suffix once the list is exhausted, so organisations never share a base name.
   * `random` is injectable (defaults to Math.random) so the output can be made deterministic.
   */
  static buildRandomRegistrations(count: number, random: () => number = Math.random): TeamRegistration[] {
    const pick = <T>(items: T[]): T => items[Math.floor(random() * items.length)];
    const registrations: TeamRegistration[] = [];
    let remaining = count;
    while (remaining > 0) {
      const index = registrations.length;
      const size = Math.min(remaining, 1 + Math.floor(random() * MAX_TEAMS_PER_ORGANISATION));
      const cycle = Math.floor(index / RANDOM_TEAM_NAMES.length);
      const baseName = RANDOM_TEAM_NAMES[index % RANDOM_TEAM_NAMES.length];
      registrations.push({
        name: cycle === 0 ? baseName : `${baseName} ${cycle + 1}`,
        responsibleFirstName: pick(RANDOM_FIRST_NAMES),
        responsibleLastName: pick(RANDOM_LAST_NAMES),
        count: size,
        paid: Array.from({ length: size }, () => random() < 0.5),
      });
      remaining -= size;
    }
    return registrations;
  }

  /**
   * Builds a random score (0 to MAX_RANDOM_SCORE each side, draws allowed) for every match
   * that has no result yet, for test data. Matches already played are left untouched.
   * `random` is injectable (defaults to Math.random) so the output can be made deterministic.
   */
  static buildRandomResults(schedule: Schedule | undefined, random: () => number = Math.random): RandomMatchResult[] {
    if (!schedule) return [];
    const randomScore = () => Math.floor(random() * (MAX_RANDOM_SCORE + 1));
    return schedule.rounds
      .flatMap(round => round.matches)
      .filter(match => match.result === null)
      .map(match => ({ matchId: match.id, score1: randomScore(), score2: randomScore() }));
  }

  /**
   * Builds a random score for every knockout match that is ready to be played: both teams known
   * and no result yet. Knockout matches can't end in a draw, so on a tie one side (picked at
   * random) gets one extra point. Matches still waiting for a team are skipped: they become
   * playable once the earlier round's results are in, so callers repeat this round by round.
   */
  static buildRandomBracketResults(rounds: BracketRound[], random: () => number = Math.random): RandomMatchResult[] {
    const randomScore = () => Math.floor(random() * (MAX_RANDOM_SCORE + 1));
    return rounds
      .flatMap(round => round.matches)
      .filter(match => match.team1 !== null && match.team2 !== null && match.result === null)
      .map(match => {
        const score1 = randomScore();
        const score2 = randomScore();
        if (score1 !== score2) return { matchId: match.id, score1, score2 };
        return random() < 0.5
          ? { matchId: match.id, score1: score1 + 1, score2 }
          : { matchId: match.id, score1, score2: score2 + 1 };
      });
  }
}
