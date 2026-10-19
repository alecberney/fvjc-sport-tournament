import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';
import { TournamentCardView, TournamentListPageView } from '@app/domain/tournament/tournament.view';
import { TournamentDomainService } from '@app/domain/tournament/tournament.domain.service';

export class TournamentViewMapper {

  static readonly STATUS_LABELS: Record<TournamentStatus, string> = {
    [TournamentStatus.DRAFT]: 'Brouillon',
    [TournamentStatus.IN_PROGRESS]: 'En cours',
    [TournamentStatus.FINISHED]: 'Terminé',
  };

  static toTournamentListPageView(tournaments: Tournament[]): TournamentListPageView {
    return {
      tournaments: tournaments.map(TournamentViewMapper.toTournamentCardView),
      isEmpty: tournaments.length === 0,
    };
  }

  static toTournamentCardView(tournament: Tournament): TournamentCardView {
    return {
      id: tournament.id,
      name: tournament.name,
      sport: tournament.sport,
      date: tournament.date,
      fieldsLabel: `${tournament.numberOfFields} terrain(s)`,
      playersPerTeamLabel: `${TournamentViewMapper.toPlayersPerTeamRange(tournament)} joueurs/équipe`,
      statusLabel: TournamentViewMapper.STATUS_LABELS[tournament.status],
      isStarted: !TournamentDomainService.isDraft(tournament),
    };
  }

  // "2–4" for a range, a single "2" when min and max are equal.
  private static toPlayersPerTeamRange(tournament: Tournament): string {
    const { minPlayersPerTeam: min, maxPlayersPerTeam: max } = tournament;
    return min === max ? `${min}` : `${min}–${max}`;
  }
}
