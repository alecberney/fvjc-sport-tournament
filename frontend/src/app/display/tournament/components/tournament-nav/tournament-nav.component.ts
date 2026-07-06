import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-tournament-nav',
  templateUrl: './tournament-nav.component.html',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MatTabsModule, MatIconModule],
})
export class TournamentNavComponent {
  @Input({ required: true }) tournamentId!: string;
}
