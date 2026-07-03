import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';
import { Schedule } from '@app/domain/schedule/schedule.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { ScheduleState } from '@app/domain/schedule/schedule.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { ScheduleGenerateModal } from '@app/display/tournament/pages/schedule-generate/schedule-generate.modal';

@Component({
  selector: 'app-tournament-schedule-page',
  templateUrl: './tournament-schedule.page.html',
  styleUrl: './tournament-schedule.page.scss',
  standalone: true,
  imports: [AsyncPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule],
})
export class TournamentSchedulePage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly schedule$: Observable<Schedule | undefined> = this.store.select(ScheduleState.getSchedule);

  private tournamentId!: string;

  protected isDraft(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.DRAFT;
  }

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([new LoadTournamentById(this.tournamentId), new LoadSchedule(this.tournamentId)]);
  }

  openGenerateModal(): void {
    this.dialog.open(ScheduleGenerateModal, {
      data: { tournamentId: this.tournamentId },
    });
  }
}
