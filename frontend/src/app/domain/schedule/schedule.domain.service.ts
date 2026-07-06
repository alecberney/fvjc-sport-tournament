import { Injectable } from '@angular/core';
import { Schedule } from '@app/domain/schedule/schedule.model';

@Injectable({ providedIn: 'root' })
export class ScheduleDomainService {

  static hasAllResults(schedule: Schedule | undefined): boolean {
    if (!schedule) return false;
    return schedule.rounds.every(round =>
      round.matches.every(match => match.result !== null),
    );
  }
}
