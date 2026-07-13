import { DatePipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { Tournament } from '@app/domain/tournament/tournament.model';

@Component({
  selector: 'app-tournament-header',
  templateUrl: './tournament-header.component.html',
  styleUrl: './tournament-header.component.scss',
  standalone: true,
  imports: [DatePipe],
})
export class TournamentHeaderComponent {
  @Input({ required: true }) tournament!: Tournament;
}
