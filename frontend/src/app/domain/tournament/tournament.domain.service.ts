import { Injectable } from '@angular/core';
import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';

// Tournament lifecycle rules: DRAFT (setup) → IN_PROGRESS (results entry) → FINISHED (locked).
@Injectable({ providedIn: 'root' })
export class TournamentDomainService {

  static isDraft(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.DRAFT;
  }

  static isInProgress(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.IN_PROGRESS;
  }

  static isFinished(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.FINISHED;
  }

  static canStart(tournament: Tournament, hasSchedule: boolean): boolean {
    return TournamentDomainService.isDraft(tournament) && hasSchedule;
  }

  static canEnd(tournament: Tournament, isFinalPlayed: boolean): boolean {
    return TournamentDomainService.isInProgress(tournament) && isFinalPlayed;
  }

  // Results can only be entered while the tournament runs: not before the start, not once ended.
  static canEnterResults(tournament: Tournament): boolean {
    return TournamentDomainService.isInProgress(tournament);
  }
}
