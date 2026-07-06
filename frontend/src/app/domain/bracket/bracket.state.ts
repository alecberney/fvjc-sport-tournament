import { Injectable, inject } from '@angular/core';
import { State, Action, StateContext, Selector } from '@ngxs/store';
import { switchMap, tap } from 'rxjs';
import { BracketApiService } from '@app/api/bracket/bracket.api.service';
import { BracketApiMapper } from '@app/api/bracket/bracket.api.mapper';
import { BracketRound } from './bracket.model';
import { EnterBracketMatchResult, GenerateBracket, LoadBracket } from './bracket.actions';

export interface IBracketState {
  rounds: BracketRound[];
}

@State<IBracketState>({
  name: 'bracket',
  defaults: { rounds: [] },
})
@Injectable()
export class BracketState {
  private readonly bracketApiService = inject(BracketApiService);

  @Selector()
  static getRounds(state: IBracketState): BracketRound[] {
    return state.rounds;
  }

  @Selector()
  static hasBracket(state: IBracketState): boolean {
    return state.rounds.length > 0;
  }

  @Action(LoadBracket)
  loadBracket(ctx: StateContext<IBracketState>, { tournamentId }: LoadBracket) {
    return this.bracketApiService.loadBracket$(tournamentId).pipe(
      tap((dtos) => {
        ctx.patchState({ rounds: dtos.map(BracketApiMapper.toRoundDomain) });
      }),
    );
  }

  @Action(GenerateBracket)
  generateBracket(ctx: StateContext<IBracketState>, { tournamentId, request }: GenerateBracket) {
    return this.bracketApiService.generateBracket$(tournamentId, request).pipe(
      tap((dtos) => {
        ctx.patchState({ rounds: dtos.map(BracketApiMapper.toRoundDomain) });
      }),
    );
  }

  @Action(EnterBracketMatchResult)
  enterMatchResult(ctx: StateContext<IBracketState>, { tournamentId, matchId, score1, score2 }: EnterBracketMatchResult) {
    const request = BracketApiMapper.toSubmitResultRequest(score1, score2);
    return this.bracketApiService.submitBracketMatchResult$(tournamentId, matchId, request).pipe(
      switchMap(() => ctx.dispatch(new LoadBracket(tournamentId))),
    );
  }
}
