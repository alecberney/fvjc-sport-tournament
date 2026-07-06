export class StartTournament {
  static readonly type = '[Result] Start Tournament';
  constructor(public readonly tournamentId: string) {}
}

export class SubmitResult {
  static readonly type = '[Result] Submit Result';
  constructor(
    public readonly tournamentId: string,
    public readonly matchId: string,
    public readonly score1: number,
    public readonly score2: number,
  ) {}
}

export class LoadGroupRanking {
  static readonly type = '[Result] Load Group Ranking';
  constructor(
    public readonly tournamentId: string,
    public readonly groupId: string,
  ) {}
}

export class UpdateMatchResult {
  static readonly type = '[Result] Update Match Result';
  constructor(
    public readonly matchId: string,
    public readonly score1: number,
    public readonly score2: number,
  ) {}
}
