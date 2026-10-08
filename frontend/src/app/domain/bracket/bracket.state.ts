import { Injectable, inject } from '@angular/core';
import { State, Action, StateContext, Selector } from '@ngxs/store';
import { concatMap, from, last, map, Observable, of, switchMap, tap } from 'rxjs';
import { BracketApiService } from '@app/api/bracket/bracket.api.service';
import { BracketApiMapper } from '@app/api/bracket/bracket.api.mapper';
import { BracketRound } from './bracket.model';
import { EnterBracketMatchResult, GenerateBracket, GenerateRandomBracketResults, LoadBracket } from './bracket.actions';
import { SeedingDomainService } from '@app/domain/seeding/seeding.domain.service';

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

  @Action(GenerateRandomBracketResults)
  generateRandomBracketResults(ctx: StateContext<IBracketState>, { tournamentId }: GenerateRandomBracketResults) {
    return this.playRandomRounds$(tournamentId).pipe(
      tap((rounds) => ctx.patchState({ rounds })),
    );
  }

  // Loads the bracket, submits random results for every playable match, then recurses: winners
  // advance into the next round on the backend, which makes new matches playable. Stops when
  // nothing is left to play (final and 3rd place done) and returns the final bracket.
  private playRandomRounds$(tournamentId: string): Observable<BracketRound[]> {
    return this.bracketApiService.loadBracket$(tournamentId).pipe(
      map((dtos) => dtos.map(BracketApiMapper.toRoundDomain)),
      switchMap((rounds) => {
        const results = SeedingDomainService.buildRandomBracketResults(rounds);
        if (results.length === 0) return of(rounds);
        return from(results).pipe(
          concatMap(({ matchId, score1, score2 }) => this.bracketApiService.submitBracketMatchResult$(
            tournamentId, matchId, BracketApiMapper.toSubmitResultRequest(score1, score2))),
          last(),
          switchMap(() => this.playRandomRounds$(tournamentId)),
        );
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
