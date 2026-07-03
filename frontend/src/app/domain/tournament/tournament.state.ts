import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { Tournament } from '@app/domain/tournament/tournament.model';
import {
  CreateTournament,
  DeleteTournament,
  LoadTournamentById,
  LoadTournaments,
  PatchSelectedStatus,
} from '@app/domain/tournament/tournament.actions';
import { TournamentApiService } from '@app/api/tournament/tournament.api.service';
import { TournamentApiMapper } from '@app/api/tournament/tournament.api.mapper';

export interface ITournamentState {
  tournaments: Tournament[];
  selected: Tournament | undefined;
}

@State<ITournamentState>({
  name: 'tournament',
  defaults: {
    tournaments: [],
    selected: undefined,
  },
})
@Injectable()
export class TournamentState {

  private readonly tournamentApiService = inject(TournamentApiService);

  @Selector()
  static getTournaments(state: ITournamentState): Tournament[] {
    return state.tournaments;
  }

  @Selector()
  static getSelected(state: ITournamentState): Tournament | undefined {
    return state.selected;
  }

  @Action(LoadTournaments)
  loadTournaments(ctx: StateContext<ITournamentState>) {
    return this.tournamentApiService.getAll$().pipe(
      tap((dtos) => {
        ctx.patchState({ tournaments: dtos.map(TournamentApiMapper.toDomain) });
      })
    );
  }

  @Action(LoadTournamentById)
  loadTournamentById(ctx: StateContext<ITournamentState>, { id }: LoadTournamentById) {
    return this.tournamentApiService.getById$(id).pipe(
      tap((dto) => {
        ctx.patchState({ selected: TournamentApiMapper.toDomain(dto) });
      })
    );
  }

  @Action(CreateTournament)
  createTournament(ctx: StateContext<ITournamentState>, { tournament }: CreateTournament) {
    return this.tournamentApiService.create$(TournamentApiMapper.toCreateRequest(tournament)).pipe(
      tap((dto) => {
        const created = TournamentApiMapper.toDomain(dto);
        ctx.patchState({
          tournaments: [...ctx.getState().tournaments, created],
          selected: created,
        });
      })
    );
  }

  @Action(DeleteTournament)
  deleteTournament(ctx: StateContext<ITournamentState>, { id }: DeleteTournament) {
    return this.tournamentApiService.delete$(id).pipe(
      tap(() => {
        ctx.patchState({
          tournaments: ctx.getState().tournaments.filter((t) => t.id !== id),
        });
      })
    );
  }

  @Action(PatchSelectedStatus)
  patchSelectedStatus(ctx: StateContext<ITournamentState>, { status }: PatchSelectedStatus) {
    const selected = ctx.getState().selected;
    if (selected) {
      ctx.patchState({ selected: { ...selected, status } });
    }
  }
}
