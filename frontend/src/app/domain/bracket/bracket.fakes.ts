import { BracketMatch, BracketRound } from '@app/domain/bracket/bracket.model';

export class BracketFakes {

  static aMatch(overrides?: Partial<BracketMatch>): BracketMatch {
    return {
      id: 'bracket-match-id-1',
      field: 1,
      team1: { id: 'team-id-1', name: 'Les Aigles' },
      team2: { id: 'team-id-2', name: 'Les Loups' },
      result: null,
      ...overrides,
    };
  }

  static aRound(overrides?: Partial<BracketRound>): BracketRound {
    return {
      id: 'bracket-round-id-1',
      number: 1,
      name: 'FINALE',
      startTime: new Date('2025-08-15T16:00:00'),
      matches: [BracketFakes.aMatch()],
      ...overrides,
    };
  }
}
