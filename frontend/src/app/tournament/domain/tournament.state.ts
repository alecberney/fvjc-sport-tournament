import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { Tournament } from './tournament.model';
import {
  CreateTournament,
  DeleteTournament,
  LoadTournamentById,
  LoadTournaments,
} from './tournament.actions';
import { TournamentApiService } from '../api/tournament.api.service';

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

  private readonly apiService = inject(TournamentApiService);

  @Selector()
  static getTournaments(state: ITournamentState): Tournament[] {
    return state.tournaments;
  }

  @Selector()
  static getSelected(state: ITournamentState): Tournament | undefined {
    return state.selected;
  }

  @Action(LoadTournaments)
  loadTournaments(_ctx: StateContext<ITournamentState>): void {}

  @Action(LoadTournamentById)
  loadTournamentById(_ctx: StateContext<ITournamentState>, _action: LoadTournamentById): void {}

  @Action(CreateTournament)
  createTournament(_ctx: StateContext<ITournamentState>, _action: CreateTournament): void {}

  @Action(DeleteTournament)
  deleteTournament(_ctx: StateContext<ITournamentState>, _action: DeleteTournament): void {}
}
