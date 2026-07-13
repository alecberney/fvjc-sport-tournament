import { AsyncPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { Group } from '@app/domain/group/group.model';
import { GroupRanking } from '@app/domain/result/result.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { GroupState } from '@app/domain/group/group.state';
import { ResultState } from '@app/domain/result/result.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadGroups } from '@app/domain/group/group.actions';
import { LoadAllGroupRankings } from '@app/domain/result/result.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';

@Component({
  selector: 'app-tournament-rankings-page',
  templateUrl: './tournament-rankings.page.html',
  styleUrl: './tournament-rankings.page.scss',
  standalone: true,
  imports: [AsyncPipe, FormsModule, MatFormFieldModule, MatSelectModule, TournamentNavComponent, TournamentHeaderComponent],
})
export class TournamentRankingsPage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly groups$: Observable<Group[]> = this.store.select(GroupState.getGroups);
  readonly rankings$: Observable<{ [groupId: string]: GroupRanking }> = this.store.select(ResultState.getRankings);

  selectedGroup: string | null = null;
  protected tournamentId!: string;

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([
      new LoadTournamentById(this.tournamentId),
      new LoadGroups(this.tournamentId),
      new LoadAllGroupRankings(this.tournamentId, {}),
    ]);
  }

  onGroupFilterChange(): void {
    this.store.dispatch(new LoadAllGroupRankings(
      this.tournamentId,
      { groups: this.selectedGroup ?? undefined },
    ));
  }

  getRankingsToDisplay(groups: Group[], rankings: { [groupId: string]: GroupRanking }): GroupRanking[] {
    if (this.selectedGroup) {
      const group = groups.find(g => g.name === this.selectedGroup);
      if (!group) return [];
      const ranking = rankings[group.id];
      return ranking ? [ranking] : [];
    }
    return groups
      .map(g => rankings[g.id])
      .filter((r): r is GroupRanking => r !== undefined);
  }
}
