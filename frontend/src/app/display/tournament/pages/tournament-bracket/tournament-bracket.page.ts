import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, Input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';
import { BracketMatch, BracketRound } from '@app/domain/bracket/bracket.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { BracketState } from '@app/domain/bracket/bracket.state';
import { ScheduleState } from '@app/domain/schedule/schedule.state';
import { EndTournament, LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { EnterBracketMatchResult, LoadBracket } from '@app/domain/bracket/bracket.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';
import { TournamentActionsComponent } from '@app/display/tournament/components/tournament-actions/tournament-actions.component';
import { BracketGenerateModal } from '@app/display/tournament/components/bracket-generate-modal/bracket-generate-modal.component';
import { TournamentEndConfirmModal } from '@app/display/tournament/components/tournament-end-confirm/tournament-end-confirm.modal';
import { ScoreFormComponent, ScoreFormValue } from '@app/display/tournament/components/score-form/score-form.component';

@Component({
  selector: 'app-tournament-bracket-page',
  templateUrl: './tournament-bracket.page.html',
  styleUrl: './tournament-bracket.page.scss',
  standalone: true,
  imports: [
    AsyncPipe,
    DatePipe,
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

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly rounds$: Observable<BracketRound[]> = this.store.select(BracketState.getRounds);
  readonly hasBracket$: Observable<boolean> = this.store.select(BracketState.hasBracket);
  readonly hasAllResults$: Observable<boolean> = this.store.select(ScheduleState.hasAllResults);
  readonly isFinalPlayed$: Observable<boolean> = this.store.select(BracketState.isFinalPlayed);

  @Input({ alias: 'id', required: true }) tournamentId!: string;
  selectedMatch: BracketMatch | null = null;

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

  protected isFinished(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.FINISHED;
  }

  protected canEndTournament(tournament: Tournament, isFinalPlayed: boolean | null): boolean {
    return tournament.status === TournamentStatus.IN_PROGRESS && !!isFinalPlayed;
  }

  protected isPlayable(match: BracketMatch): boolean {
    return !!match.team1 && !!match.team2;
  }

  // Results are locked once the tournament has ended.
  protected canSelectMatch(tournament: Tournament, match: BracketMatch): boolean {
    return tournament.status === TournamentStatus.IN_PROGRESS && this.isPlayable(match);
  }

  openEndModal(): void {
    this.dialog.open(TournamentEndConfirmModal).afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.store.dispatch(new EndTournament(this.tournamentId)).subscribe(() => {
        this.selectedMatch = null;
      });
    });
  }

  selectMatch(tournament: Tournament, match: BracketMatch): void {
    if (!this.canSelectMatch(tournament, match)) return;
    this.selectedMatch = match;
  }

  submitResult({ score1, score2 }: ScoreFormValue): void {
    if (!this.selectedMatch) return;
    this.store.dispatch(
      new EnterBracketMatchResult(this.tournamentId, this.selectedMatch.id, score1, score2),
    ).subscribe(() => {
      this.selectedMatch = null;
    });
  }
}
