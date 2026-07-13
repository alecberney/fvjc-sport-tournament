import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Schedule, Match } from '@app/domain/schedule/schedule.model';
import { GroupRanking } from '@app/domain/result/result.model';
import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { ScheduleState } from '@app/domain/schedule/schedule.state';
import { ResultState } from '@app/domain/result/result.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { SubmitResult } from '@app/domain/result/result.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';

@Component({
  selector: 'app-tournament-results-page',
  templateUrl: './tournament-results.page.html',
  styleUrl: './tournament-results.page.scss',
  standalone: true,
  imports: [AsyncPipe, DatePipe, FormsModule, RouterLink, MatButtonModule, MatIconModule, MatInputModule, MatFormFieldModule, TournamentNavComponent, TournamentHeaderComponent],
})
export class TournamentResultsPage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly schedule$: Observable<Schedule | undefined> = this.store.select(ScheduleState.getSchedule);
  readonly rankings$: Observable<{ [groupId: string]: GroupRanking }> = this.store.select(ResultState.getRankings);

  selectedMatchId: string | null = null;
  score1: number | null = null;
  score2: number | null = null;

  protected tournamentId!: string;

  protected isDraft(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.DRAFT;
  }

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([
      new LoadTournamentById(this.tournamentId),
      new LoadSchedule(this.tournamentId),
    ]);
  }

  selectMatch(match: Match): void {
    this.selectedMatchId = match.id;
    this.score1 = match.result?.score1 ?? null;
    this.score2 = match.result?.score2 ?? null;
  }

  submitResult(): void {
    if (this.selectedMatchId == null || this.score1 == null || this.score2 == null) return;
    this.store.dispatch(new SubmitResult(this.tournamentId, this.selectedMatchId, this.score1, this.score2));
  }

  getSelectedMatch(schedule: Schedule | undefined): Match | null {
    if (!schedule || !this.selectedMatchId) return null;
    for (const round of schedule.rounds) {
      const found = round.matches.find(m => m.id === this.selectedMatchId);
      if (found) return found;
    }
    return null;
  }

  getRankingForMatch(match: Match, rankings: { [groupId: string]: GroupRanking }): GroupRanking | null {
    return rankings[match.groupId] ?? null;
  }
}
