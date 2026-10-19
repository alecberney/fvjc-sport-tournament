import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';

interface TournamentNavTab {
  path: string[];
  label: string;
  icon: string;
  exact: boolean;
}

@Component({
  selector: 'app-tournament-nav',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tournament-nav.component.html',
  styleUrl: './tournament-nav.component.scss',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MatTabsModule, MatIconModule],
})
export class TournamentNavComponent {
  @Input({ required: true }) tournamentId!: string;

  // Path segments are relative to the tournament root ('/', tournamentId).
  // Only the root tab (Équipes) needs exact matching, otherwise it would stay
  // active on every child route.
  protected readonly tabs: readonly TournamentNavTab[] = [
    { path: [], label: 'Équipes', icon: 'groups', exact: true },
    { path: ['groups'], label: 'Groupes', icon: 'grid_view', exact: false },
    { path: ['schedule'], label: 'Horaire', icon: 'calendar_month', exact: false },
    { path: ['results'], label: 'Résultats', icon: 'emoji_events', exact: false },
    { path: ['rankings'], label: 'Classements', icon: 'leaderboard', exact: false },
    { path: ['bracket'], label: 'Bracket', icon: 'account_tree', exact: false },
  ];
}
