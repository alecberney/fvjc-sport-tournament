import { Match, Round, Schedule } from '@app/domain/schedule/schedule.model';

export class ScheduleFakes {

  static aMatch(overrides?: Partial<Match>): Match {
    return {
      id: 'match-id-1',
      field: 1,
      groupId: 'group-id-1',
      groupName: 'Groupe A',
      team1: { id: 'team-id-1', name: 'Les Aigles' },
      team2: { id: 'team-id-2', name: 'Les Lions' },
      result: { score1: 21, score2: 15 },
      ...overrides,
    };
  }

  static aRound(overrides?: Partial<Round>): Round {
    return {
      id: 'round-id-1',
      number: 1,
      startTime: new Date('2025-08-15T09:00:00'),
      matches: [ScheduleFakes.aMatch()],
      ...overrides,
    };
  }

  static aSchedule(overrides?: Partial<Schedule>): Schedule {
    return {
      totalRounds: 1,
      totalMatches: 1,
      rounds: [ScheduleFakes.aRound()],
      ...overrides,
    };
  }
}
