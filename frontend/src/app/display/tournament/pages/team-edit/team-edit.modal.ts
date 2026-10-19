import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Team } from '@app/domain/team/team.model';
import { UpdateTeam } from '@app/domain/team/team.actions';

@Component({
  selector: 'app-team-edit-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './team-edit.modal.html',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
  ],
})
export class TeamEditModal implements OnInit {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TeamEditModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string; team: Team }>(MAT_DIALOG_DATA);
  private readonly destroyRef = inject(DestroyRef);

  readonly form = this.fb.group({
    name: [this.data.team.name, [Validators.required, Validators.maxLength(250)]],
    responsibleFirstName: [this.data.team.responsibleFirstName, [Validators.required, Validators.maxLength(100)]],
    responsibleLastName: [this.data.team.responsibleLastName, [Validators.required, Validators.maxLength(100)]],
    paid: [this.data.team.paid],
  });

  ngOnInit(): void {
    this.actions$.pipe(
      ofActionSuccessful(UpdateTeam),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.dialogRef.close());
  }

  submit(): void {
    if (this.form.invalid) return;
    const { name, responsibleFirstName, responsibleLastName, paid } = this.form.getRawValue();
    this.store.dispatch(new UpdateTeam(this.data.tournamentId, this.data.team.id, {
      name: name!,
      responsibleFirstName: responsibleFirstName!,
      responsibleLastName: responsibleLastName!,
      paid: paid!,
    }));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
