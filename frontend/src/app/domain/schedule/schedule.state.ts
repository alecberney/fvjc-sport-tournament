import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { Schedule } from '@app/domain/schedule/schedule.model';
import { GenerateSchedule, LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { ScheduleApiService } from '@app/api/schedule/schedule.api.service';
import { ScheduleApiMapper } from '@app/api/schedule/schedule.api.mapper';
import { UpdateMatchResult } from '@app/domain/result/result.actions';
import { ScheduleDomainService } from '@app/domain/schedule/schedule.domain.service';

export interface IScheduleState {
  schedule: Schedule | undefined;
}

@State<IScheduleState>({
  name: 'schedule',
  defaults: { schedule: undefined },
})
@Injectable()
export class ScheduleState {

  private readonly scheduleApiService = inject(ScheduleApiService);

  @Selector()
  static getSchedule(state: IScheduleState): Schedule | undefined {
    return state.schedule;
  }

  @Selector()
  static hasAllResults(state: IScheduleState): boolean {
    return ScheduleDomainService.hasAllResults(state.schedule);
  }

  @Action(LoadSchedule)
  loadSchedule(ctx: StateContext<IScheduleState>, { tournamentId }: LoadSchedule) {
    return this.scheduleApiService.getSchedule$(tournamentId).pipe(
      tap(dto => {
        ctx.patchState({ schedule: ScheduleApiMapper.toDomain(dto) });
      })
    );
  }

  @Action(GenerateSchedule)
  generateSchedule(ctx: StateContext<IScheduleState>, { tournamentId, startTime, matchDurationMinutes, breakDurationMinutes }: GenerateSchedule) {
    return this.scheduleApiService.generate$(tournamentId, { startTime, matchDurationMinutes, breakDurationMinutes }).pipe(
      tap(dto => {
        ctx.patchState({ schedule: ScheduleApiMapper.toDomain(dto) });
      })
    );
  }

  @Action(UpdateMatchResult)
  updateMatchResult(ctx: StateContext<IScheduleState>, { matchId, score1, score2 }: UpdateMatchResult) {
    const schedule = ctx.getState().schedule;
    if (!schedule) return;
    const updatedRounds = schedule.rounds.map(round => ({
      ...round,
      matches: round.matches.map(match =>
        match.id === matchId
          ? { ...match, result: { score1, score2 } }
          : match
      ),
    }));
    ctx.patchState({ schedule: { ...schedule, rounds: updatedRounds } });
  }
}
