import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { concatMap, from, last, map, of, switchMap, tap } from 'rxjs';
import { GroupRanking } from '@app/domain/result/result.model';
import { GenerateRandomResults, LoadAllGroupRankings, LoadGroupRanking, StartTournament, SubmitResult, UpdateMatchResult } from '@app/domain/result/result.actions';
import { ResultApiService } from '@app/api/result/result.api.service';
import { ResultApiMapper } from '@app/api/result/result.api.mapper';
import { PatchSelectedStatus } from '@app/domain/tournament/tournament.actions';
import { ScheduleApiService } from '@app/api/schedule/schedule.api.service';
import { ScheduleApiMapper } from '@app/api/schedule/schedule.api.mapper';
import { SeedingDomainService } from '@app/domain/seeding/seeding.domain.service';

export interface IResultState {
  rankings: { [groupId: string]: GroupRanking };
}

@State<IResultState>({
  name: 'result',
  defaults: { rankings: {} },
})
@Injectable()
export class ResultState {

  private readonly resultApiService = inject(ResultApiService);
  private readonly scheduleApiService = inject(ScheduleApiService);

  @Selector()
  static getRankings(state: IResultState): { [groupId: string]: GroupRanking } {
    return state.rankings;
  }

  @Selector()
  static getRankingForGroup(state: IResultState): (groupId: string) => GroupRanking | undefined {
    return (groupId: string) => state.rankings[groupId];
  }

  @Action(StartTournament)
  startTournament(ctx: StateContext<IResultState>, { tournamentId }: StartTournament) {
    return this.resultApiService.startTournament$(tournamentId).pipe(
      tap((dto) => {
        ctx.dispatch(new PatchSelectedStatus(dto.status));
      })
    );
  }

  @Action(SubmitResult)
  submitResult(ctx: StateContext<IResultState>, { tournamentId, matchId, score1, score2 }: SubmitResult) {
    return this.resultApiService.submitResult$(tournamentId, matchId, { score1, score2 }).pipe(
      tap((dto) => {
        const ranking = ResultApiMapper.toGroupRankingDomain(dto.ranking);
        ctx.patchState({
          rankings: { ...ctx.getState().rankings, [ranking.groupId]: ranking },
        });
        if (dto.match.result) {
          ctx.dispatch(new UpdateMatchResult(dto.match.id, dto.match.result.score1, dto.match.result.score2));
        }
      })
    );
  }

  // Reloads the schedule so it works from any page, then submits a random score for each
  // unplayed match one after the other through SubmitResult (keeps rankings and schedule in sync).
  @Action(GenerateRandomResults)
  generateRandomResults(ctx: StateContext<IResultState>, { tournamentId }: GenerateRandomResults) {
    return this.scheduleApiService.getSchedule$(tournamentId).pipe(
      map(dto => SeedingDomainService.buildRandomResults(ScheduleApiMapper.toDomain(dto))),
      switchMap(results => results.length === 0
        ? of(undefined)
        : from(results).pipe(
          concatMap(({ matchId, score1, score2 }) => ctx.dispatch(new SubmitResult(tournamentId, matchId, score1, score2))),
          last(),
        )),
    );
  }

  @Action(LoadGroupRanking)
  loadGroupRanking(ctx: StateContext<IResultState>, { tournamentId, groupId }: LoadGroupRanking) {
    return this.resultApiService.loadGroupRanking$(tournamentId, groupId).pipe(
      tap((dto) => {
        const ranking = ResultApiMapper.toGroupRankingDomain(dto);
        ctx.patchState({
          rankings: { ...ctx.getState().rankings, [ranking.groupId]: ranking },
        });
      })
    );
  }

  @Action(LoadAllGroupRankings)
  loadAllGroupRankings(ctx: StateContext<IResultState>, { tournamentId, request }: LoadAllGroupRankings) {
    return this.resultApiService.loadAllGroupRankings$(tournamentId, request).pipe(
      tap((dtos) => {
        const newRankings = { ...ctx.getState().rankings };
        dtos.forEach(dto => {
          const ranking = ResultApiMapper.toGroupRankingDomain(dto);
          newRankings[ranking.groupId] = ranking;
        });
        ctx.patchState({ rankings: newRankings });
      })
    );
  }
}
