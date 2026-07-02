import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament.state';
import { TournamentListPage } from '../display/pages/tournament-list/tournament-list.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState])],
    children: [
      { path: '', component: TournamentListPage },
    ],
  },
];
