import { Injectable } from '@angular/core';
import { Match, Schedule } from '@app/domain/schedule/schedule.model';

@Injectable({ providedIn: 'root' })
export class ScheduleDomainService {

  static hasAllResults(schedule: Schedule | undefined): boolean {
    if (!schedule) return false;
    return schedule.rounds.every(round =>
      round.matches.every(match => match.result !== null),
    );
  }

  static hasRounds(schedule: Schedule | undefined): boolean {
    return !!schedule && schedule.totalRounds > 0;
  }

  static findMatch(schedule: Schedule | undefined, matchId: string | null): Match | undefined {
    if (!schedule || !matchId) return undefined;
    return schedule.rounds.flatMap(round => round.matches).find(match => match.id === matchId);
  }
}
