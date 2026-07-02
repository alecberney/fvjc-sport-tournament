import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () =>
      import('./tournament/modules/tournament.routes').then(m => m.TOURNAMENT_ROUTES),
  },
];
