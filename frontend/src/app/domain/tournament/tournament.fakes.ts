import { Sport, Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';

export class TournamentFakes {

  static aTournament(overrides?: Partial<Tournament>): Tournament {
    return {
      id: 'tournament-id-1',
      name: 'Tournoi FVJC 2027',
      sport: Sport.PETANQUE,
      numberOfFields: 4,
      minPlayersPerTeam: 2,
      maxPlayersPerTeam: 4,
      date: new Date('2027-08-15'),
      status: TournamentStatus.DRAFT,
      ...overrides,
    };
  }

  static aDraftTournament(): Tournament {
    return TournamentFakes.aTournament({ status: TournamentStatus.DRAFT });
  }

  static anInProgressTournament(): Tournament {
    return TournamentFakes.aTournament({ status: TournamentStatus.IN_PROGRESS });
  }

  static aFinishedTournament(): Tournament {
    return TournamentFakes.aTournament({ status: TournamentStatus.FINISHED });
  }
}
