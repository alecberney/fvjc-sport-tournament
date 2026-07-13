import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
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
import { StartTournament } from '@app/domain/result/result.actions';
import { ScheduleGenerateModal } from '@app/display/tournament/pages/schedule-generate/schedule-generate.modal';
import { TournamentStartConfirmModal } from './tournament-start-confirm.modal';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';

@Component({
  selector: 'app-tournament-schedule-page',
  templateUrl: './tournament-schedule.page.html',
  styleUrl: './tournament-schedule.page.scss',
  standalone: true,
  imports: [AsyncPipe, DatePipe, MatButtonModule, MatIconModule, TournamentNavComponent, TournamentHeaderComponent],
})
export class TournamentSchedulePage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly schedule$: Observable<Schedule | undefined> = this.store.select(ScheduleState.getSchedule);

  protected tournamentId!: string;

  protected isDraft(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.DRAFT;
  }

  isInProgress(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.IN_PROGRESS;
  }

  startTournament(): void {
    const dialogRef = this.dialog.open(TournamentStartConfirmModal);
    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.store.dispatch(new StartTournament(this.tournamentId)).subscribe(() => {
        this.router.navigate(['/', this.tournamentId, 'results']);
      });
    });
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
