import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { Team } from '@app/domain/team/team.model';
import { UpdateTeam } from '@app/domain/team/team.actions';

@Component({
  selector: 'app-team-edit-modal',
  templateUrl: './team-edit.modal.html',
  styleUrl: './team-edit.modal.scss',
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
export class TeamEditModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TeamEditModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string; team: Team }>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;

  ngOnInit(): void {
    const { team } = this.data;
    this.form = this.fb.group({
      name: [team.name, [Validators.required, Validators.maxLength(250)]],
      responsibleFirstName: [team.responsibleFirstName, [Validators.required, Validators.maxLength(100)]],
      responsibleLastName: [team.responsibleLastName, [Validators.required, Validators.maxLength(100)]],
      paid: [team.paid],
    });

    this.actions$.pipe(
      ofActionSuccessful(UpdateTeam),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;
    const { name, responsibleFirstName, responsibleLastName, paid } = this.form.value;
    this.store.dispatch(new UpdateTeam(this.data.tournamentId, this.data.team.id, {
      name,
      responsibleFirstName,
      responsibleLastName,
      paid,
    }));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
