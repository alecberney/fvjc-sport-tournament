import { TeamRegistration, TeamUpdate } from '@app/domain/team/team.model';

export class LoadTeams {
  static readonly type = '[Team] Load Teams';
  constructor(public readonly tournamentId: string) {}
}

export class RegisterTeams {
  static readonly type = '[Team] Register Teams';
  constructor(
    public readonly tournamentId: string,
    public readonly registration: TeamRegistration,
  ) {}
}

export class UpdateTeam {
  static readonly type = '[Team] Update Team';
  constructor(
    public readonly tournamentId: string,
    public readonly teamId: string,
    public readonly update: TeamUpdate,
  ) {}
}

export class DeleteTeam {
  static readonly type = '[Team] Delete Team';
  constructor(
    public readonly tournamentId: string,
    public readonly teamId: string,
  ) {}
}

export class MarkTeamPaid {
  static readonly type = '[Team] Mark Team Paid';
  constructor(
    public readonly tournamentId: string,
    public readonly teamId: string,
    public readonly paid: boolean,
  ) {}
}
