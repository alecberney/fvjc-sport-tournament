import { GroupRanking, GroupRankingEntry } from '@app/domain/result/result.model';

export class ResultFakes {

  static aRankingEntry(overrides?: Partial<GroupRankingEntry>): GroupRankingEntry {
    return {
      rank: 1,
      team: { id: 'team-id-1', name: 'Les Aigles' },
      played: 3,
      wins: 2,
      draws: 1,
      defeats: 0,
      goalsFor: 40,
      goalsAgainst: 25,
      goalDifference: 15,
      points: 7,
      ...overrides,
    };
  }

  static aRanking(overrides?: Partial<GroupRanking>): GroupRanking {
    return {
      groupId: 'group-id-1',
      groupName: 'A',
      entries: [ResultFakes.aRankingEntry()],
      ...overrides,
    };
  }
}
