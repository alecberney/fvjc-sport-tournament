export class LoadGroups {
  static readonly type = '[Group] Load Groups';
  constructor(public readonly tournamentId: string) {}
}

export class GenerateGroups {
  static readonly type = '[Group] Generate Groups';
  constructor(
    public readonly tournamentId: string,
    public readonly groupSize: number,
  ) {}
}

export class SwapTeams {
  static readonly type = '[Group] Swap Teams';
  constructor(
    public readonly tournamentId: string,
    public readonly teamId1: string,
    public readonly teamId2: string,
  ) {}
}
