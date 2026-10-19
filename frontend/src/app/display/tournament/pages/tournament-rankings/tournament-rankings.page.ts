import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { Store } from '@ngxs/store';
import { Observable, startWith, Subject, switchMap } from 'rxjs';
import { RankingPageView } from '@app/domain/result/result.view';
import { ResultState } from '@app/domain/result/result.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadGroups } from '@app/domain/group/group.actions';
import { LoadAllGroupRankings } from '@app/domain/result/result.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';
import { TournamentActionsComponent } from '@app/display/tournament/components/tournament-actions/tournament-actions.component';
import { RankingTableComponent } from '@app/display/tournament/components/ranking-table/ranking-table.component';

@Component({
  selector: 'app-tournament-rankings-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tournament-rankings.page.html',
  styleUrl: './tournament-rankings.page.scss',
  standalone: true,
  imports: [AsyncPipe, FormsModule, MatFormFieldModule, MatSelectModule, TournamentNavComponent, TournamentHeaderComponent, TournamentActionsComponent, RankingTableComponent],
})
export class TournamentRankingsPage implements OnInit {

  private readonly store = inject(Store);

  // Stream of the user's group filter choices (null = all groups) — feeds the view selector, never stored.
  private readonly groupSelection$ = new Subject<string | null>();

  readonly vm$: Observable<RankingPageView | null> = this.groupSelection$.pipe(
    startWith(null),
    switchMap((selectedGroupName) => this.store.select(ResultState.getRankingPageView(selectedGroupName))),
  );

  @Input({ alias: 'id', required: true }) tournamentId!: string;

  ngOnInit(): void {
    this.store.dispatch([
      new LoadTournamentById(this.tournamentId),
      new LoadGroups(this.tournamentId),
      new LoadAllGroupRankings(this.tournamentId, {}),
    ]);
  }

  onGroupFilterChange(groupName: string | null): void {
    this.groupSelection$.next(groupName);
    this.store.dispatch(new LoadAllGroupRankings(
      this.tournamentId,
      { groups: groupName ?? undefined },
    ));
  }
}
