import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { BracketRound } from '@app/domain/bracket/bracket.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { BracketState } from '@app/domain/bracket/bracket.state';
import { ScheduleState } from '@app/domain/schedule/schedule.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadBracket } from '@app/domain/bracket/bracket.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { BracketGenerateModal } from '@app/display/tournament/components/bracket-generate-modal/bracket-generate-modal.component';

@Component({
  selector: 'app-tournament-bracket-page',
  templateUrl: './tournament-bracket.page.html',
  styleUrl: './tournament-bracket.page.scss',
  standalone: true,
  imports: [AsyncPipe, DatePipe, MatButtonModule, MatIconModule, TournamentNavComponent],
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
}
