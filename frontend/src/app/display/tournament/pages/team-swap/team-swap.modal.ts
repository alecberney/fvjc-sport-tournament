import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Group } from '@app/domain/group/group.model';
import { Team } from '@app/domain/team/team.model';
import { SwapTeams } from '@app/domain/group/group.actions';

export interface TeamSwapData {
  tournamentId: string;
  team: Team;
  currentGroupName: string;
  groups: Group[];
}

@Component({
  selector: 'app-team-swap-modal',
  templateUrl: './team-swap.modal.html',
  styleUrl: './team-swap.modal.scss',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
})
export class TeamSwapModal implements OnInit {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TeamSwapModal>);
  private readonly actions$ = inject(Actions);
  readonly data = inject<TeamSwapData>(MAT_DIALOG_DATA);
  private readonly destroyRef = inject(DestroyRef);

  get otherGroups(): Group[] {
    return this.data.groups.filter(g => g.name !== this.data.currentGroupName);
  }

  ngOnInit(): void {
    this.actions$.pipe(
      ofActionSuccessful(SwapTeams),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.dialogRef.close());
  }

  swap(targetTeam: Team): void {
    this.store.dispatch(new SwapTeams(this.data.tournamentId, this.data.team.id, targetTeam.id));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
