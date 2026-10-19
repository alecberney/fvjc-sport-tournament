import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, Input, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Store } from '@ngxs/store';
import { Observable, startWith, Subject, switchMap } from 'rxjs';
import { ResultMatchView, ResultPageView } from '@app/domain/result/result.view';
import { ResultState } from '@app/domain/result/result.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { SubmitResult } from '@app/domain/result/result.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';
import { TournamentActionsComponent } from '@app/display/tournament/components/tournament-actions/tournament-actions.component';
import { RankingTableComponent } from '@app/display/tournament/components/ranking-table/ranking-table.component';
import { ScoreFormComponent, ScoreFormValue } from '@app/display/tournament/components/score-form/score-form.component';
import { DateFormatPipe } from '@app/display/shared/pipes/date-format.pipe';

@Component({
  selector: 'app-tournament-results-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tournament-results.page.html',
  styleUrl: './tournament-results.page.scss',
  standalone: true,
  imports: [AsyncPipe, DateFormatPipe, RouterLink, MatButtonModule, MatIconModule, TournamentNavComponent, TournamentHeaderComponent, TournamentActionsComponent, RankingTableComponent, ScoreFormComponent],
})
export class TournamentResultsPage implements OnInit {

  private readonly store = inject(Store);

  // Stream of the user's match selections (null = none) — feeds the view selector, never stored.
  private readonly matchSelection$ = new Subject<string | null>();

  readonly vm$: Observable<ResultPageView | null> = this.matchSelection$.pipe(
    startWith(null),
    switchMap((selectedMatchId) => this.store.select(ResultState.getResultPageView(selectedMatchId))),
  );

  @Input({ alias: 'id', required: true }) tournamentId!: string;

  ngOnInit(): void {
    this.store.dispatch([
      new LoadTournamentById(this.tournamentId),
      new LoadSchedule(this.tournamentId),
    ]);
  }

  selectMatch(match: ResultMatchView): void {
    this.matchSelection$.next(match.id);
  }

  submitResult(matchId: string, { score1, score2 }: ScoreFormValue): void {
    this.store.dispatch(new SubmitResult(this.tournamentId, matchId, score1, score2));
  }
}
