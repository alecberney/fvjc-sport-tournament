import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament/tournament.state';
import { TeamState } from '../domain/team/team.state';
import { TournamentListPage } from '../display/tournament/pages/tournament-list/tournament-list.page';
import { TournamentDetailPage } from '../display/tournament/pages/tournament-detail/tournament-detail.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState, TeamState])],
    children: [
      { path: '', component: TournamentListPage },
      { path: ':id', component: TournamentDetailPage },
    ],
  },
];
