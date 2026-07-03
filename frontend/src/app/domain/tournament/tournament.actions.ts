import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';

export class LoadTournaments {
  static readonly type = '[Tournament] Load Tournaments';
}

export class LoadTournamentById {
  static readonly type = '[Tournament] Load Tournament By Id';
  constructor(public readonly id: string) {}
}

export class CreateTournament {
  static readonly type = '[Tournament] Create Tournament';
  constructor(public readonly tournament: Partial<Tournament>) {}
}

export class DeleteTournament {
  static readonly type = '[Tournament] Delete Tournament';
  constructor(public readonly id: string) {}
}

export class PatchSelectedStatus {
  static readonly type = '[Tournament] Patch Selected Status';
  constructor(public readonly status: TournamentStatus) {}
}
