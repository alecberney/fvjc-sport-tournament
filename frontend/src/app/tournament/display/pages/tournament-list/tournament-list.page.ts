import { Component, inject, OnInit } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '../../../domain/tournament.model';
import { TournamentState } from '../../../domain/tournament.state';
import { LoadTournaments } from '../../../domain/tournament.actions';

@Component({
  selector: 'app-tournament-list-page',
  templateUrl: './tournament-list.page.html',
  standalone: true,
  imports: [AsyncPipe],
})
export class TournamentListPage implements OnInit {

  private readonly store = inject(Store);

  readonly tournaments$: Observable<Tournament[]> = this.store.select(TournamentState.getTournaments);

  ngOnInit(): void {
    this.store.dispatch(new LoadTournaments());
  }
}
