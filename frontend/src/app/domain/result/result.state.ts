import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { GroupRanking } from '@app/domain/result/result.model';
import { LoadGroupRanking, StartTournament, SubmitResult, UpdateMatchResult } from '@app/domain/result/result.actions';
import { ResultApiService } from '@app/api/result/result.api.service';
import { ResultApiMapper } from '@app/api/result/result.api.mapper';
import { PatchSelectedStatus } from '@app/domain/tournament/tournament.actions';

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
}
