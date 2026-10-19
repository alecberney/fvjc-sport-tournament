import { AsyncPipe } from '@angular/common';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Store } from '@ngxs/store';
import { Observable, switchMap } from 'rxjs';
import { BracketMatchView, BracketPageView } from '@app/domain/bracket/bracket.view';
import { BracketState } from '@app/domain/bracket/bracket.state';
import { EndTournament, LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { EnterBracketMatchResult, LoadBracket } from '@app/domain/bracket/bracket.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';
import { TournamentActionsComponent } from '@app/display/tournament/components/tournament-actions/tournament-actions.component';
import { BracketGenerateModal } from '@app/display/tournament/components/bracket-generate-modal/bracket-generate-modal.component';
import { TournamentEndConfirmModal } from '@app/display/tournament/components/tournament-end-confirm/tournament-end-confirm.modal';
import { ScoreFormComponent, ScoreFormValue } from '@app/display/tournament/components/score-form/score-form.component';
import { DateFormatPipe } from '@app/display/shared/pipes/date-format.pipe';

@Component({
  selector: 'app-tournament-bracket-page',
  templateUrl: './tournament-bracket.page.html',
  styleUrl: './tournament-bracket.page.scss',
  standalone: true,
  imports: [
    AsyncPipe,
    DateFormatPipe,
    MatButtonModule,
    MatIconModule,
    TournamentNavComponent,
    TournamentHeaderComponent, TournamentActionsComponent,
    ScoreFormComponent,
  ],
})
export class TournamentBracketPage implements OnInit {
  private readonly store = inject(Store);
  private readonly dialog = inject(MatDialog);

  // UI state owned by the page — passed to the view selector, never stored in NGXS.
  private readonly selectedMatchId = signal<string | null>(null);

  readonly vm$: Observable<BracketPageView | null> = toObservable(this.selectedMatchId).pipe(
    switchMap((selectedMatchId) => this.store.select(BracketState.getBracketPageView(selectedMatchId))),
  );

  @Input({ alias: 'id', required: true }) tournamentId!: string;

  ngOnInit(): void {
    this.store.dispatch([
      new LoadTournamentById(this.tournamentId),
      new LoadBracket(this.tournamentId),
      new LoadSchedule(this.tournamentId),
    ]);
  }

  openGenerateModal(): void {
    this.dialog.open(BracketGenerateModal, {
      data: { tournamentId: this.tournamentId },
      width: '480px',
    });
  }

  openEndModal(): void {
    this.dialog.open(TournamentEndConfirmModal).afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.store.dispatch(new EndTournament(this.tournamentId)).subscribe(() => {
        this.selectedMatchId.set(null);
      });
    });
  }

  selectMatch(match: BracketMatchView): void {
    if (!match.isSelectable) return;
    this.selectedMatchId.set(match.id);
  }

  submitResult(matchId: string, { score1, score2 }: ScoreFormValue): void {
    this.store.dispatch(
      new EnterBracketMatchResult(this.tournamentId, matchId, score1, score2),
    ).subscribe(() => {
      this.selectedMatchId.set(null);
    });
  }
}
