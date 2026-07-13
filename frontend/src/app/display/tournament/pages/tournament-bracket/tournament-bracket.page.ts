import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { BracketMatch, BracketRound } from '@app/domain/bracket/bracket.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { BracketState } from '@app/domain/bracket/bracket.state';
import { ScheduleState } from '@app/domain/schedule/schedule.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { EnterBracketMatchResult, LoadBracket } from '@app/domain/bracket/bracket.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';
import { BracketGenerateModal } from '@app/display/tournament/components/bracket-generate-modal/bracket-generate-modal.component';

@Component({
  selector: 'app-tournament-bracket-page',
  templateUrl: './tournament-bracket.page.html',
  styleUrl: './tournament-bracket.page.scss',
  standalone: true,
  imports: [
    AsyncPipe,
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TournamentNavComponent,
    TournamentHeaderComponent,
  ],
})
export class TournamentBracketPage implements OnInit {
  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly rounds$: Observable<BracketRound[]> = this.store.select(BracketState.getRounds);
  readonly hasBracket$: Observable<boolean> = this.store.select(BracketState.hasBracket);
  readonly hasAllResults$: Observable<boolean> = this.store.select(ScheduleState.hasAllResults);

  protected tournamentId!: string;
  selectedMatch: BracketMatch | null = null;
  score1: number | null = null;
  score2: number | null = null;

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
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

  selectMatch(match: BracketMatch): void {
    if (!match.team1 || !match.team2) return;
    this.selectedMatch = match;
    this.score1 = match.result?.score1 ?? null;
    this.score2 = match.result?.score2 ?? null;
  }

  submitResult(): void {
    if (!this.selectedMatch || this.score1 === null || this.score2 === null) return;
    this.store.dispatch(
      new EnterBracketMatchResult(this.tournamentId, this.selectedMatch.id, this.score1, this.score2),
    ).subscribe(() => {
      this.selectedMatch = null;
      this.score1 = null;
      this.score2 = null;
    });
  }
}
