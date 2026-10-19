import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { SchedulePageView } from '@app/domain/schedule/schedule.view';
import { ScheduleState } from '@app/domain/schedule/schedule.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { StartTournament } from '@app/domain/result/result.actions';
import { ScheduleGenerateModal } from '@app/display/tournament/pages/schedule-generate/schedule-generate.modal';
import { TournamentStartConfirmModal } from './tournament-start-confirm.modal';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';
import { TournamentActionsComponent } from '@app/display/tournament/components/tournament-actions/tournament-actions.component';
import { DateFormatPipe } from '@app/display/shared/pipes/date-format.pipe';

@Component({
  selector: 'app-tournament-schedule-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tournament-schedule.page.html',
  styleUrl: './tournament-schedule.page.scss',
  standalone: true,
  imports: [AsyncPipe, DateFormatPipe, MatButtonModule, MatIconModule, TournamentNavComponent, TournamentHeaderComponent, TournamentActionsComponent],
})
export class TournamentSchedulePage implements OnInit {

  private readonly store = inject(Store);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly vm$: Observable<SchedulePageView | null> = this.store.select(ScheduleState.getSchedulePageView());

  @Input({ alias: 'id', required: true }) tournamentId!: string;

  ngOnInit(): void {
    this.store.dispatch([new LoadTournamentById(this.tournamentId), new LoadSchedule(this.tournamentId)]);
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

  openGenerateModal(): void {
    this.dialog.open(ScheduleGenerateModal, {
      data: { tournamentId: this.tournamentId },
    });
  }
}
