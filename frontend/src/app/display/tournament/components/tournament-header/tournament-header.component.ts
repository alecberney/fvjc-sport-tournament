import { Component, Input } from '@angular/core';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { DateFormatPipe } from '@app/display/shared/pipes/date-format.pipe';

@Component({
  selector: 'app-tournament-header',
  templateUrl: './tournament-header.component.html',
  styleUrl: './tournament-header.component.scss',
  standalone: true,
  imports: [DateFormatPipe],
})
export class TournamentHeaderComponent {
  @Input({ required: true }) tournament!: Tournament;
}
