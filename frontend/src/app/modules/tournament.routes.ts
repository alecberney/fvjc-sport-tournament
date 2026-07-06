import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament/tournament.state';
import { TeamState } from '../domain/team/team.state';
import { GroupState } from '../domain/group/group.state';
import { ScheduleState } from '../domain/schedule/schedule.state';
import { ResultState } from '../domain/result/result.state';
import { TournamentListPage } from '../display/tournament/pages/tournament-list/tournament-list.page';
import { TournamentDetailPage } from '../display/tournament/pages/tournament-detail/tournament-detail.page';
import { TournamentGroupsPage } from '../display/tournament/pages/tournament-groups/tournament-groups.page';
import { TournamentSchedulePage } from '../display/tournament/pages/tournament-schedule/tournament-schedule.page';
import { TournamentResultsPage } from '../display/tournament/pages/tournament-results/tournament-results.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState, TeamState, GroupState, ScheduleState, ResultState])],
    children: [
      { path: '', component: TournamentListPage },
      { path: ':id', component: TournamentDetailPage },
      { path: ':id/groups', component: TournamentGroupsPage },
      { path: ':id/schedule', component: TournamentSchedulePage },
      { path: ':id/results', component: TournamentResultsPage },
    ],
  },
];
