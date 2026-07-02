import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { Sport } from '@app/domain/tournament/tournament.model';
import { CreateTournament } from '@app/domain/tournament/tournament.actions';

@Component({
  selector: 'app-tournament-create-modal',
  templateUrl: './tournament-create.modal.html',
  styleUrl: './tournament-create.modal.scss',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
  ],
})
export class TournamentCreateModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TournamentCreateModal>);
  private readonly formBuilder = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly destroy$ = new Subject<void>();

  readonly sports = Object.values(Sport);

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(250)]],
      sport: [null, Validators.required],
      numberOfFields: [null, [Validators.required, Validators.min(1), Validators.max(500)]],
      minPlayersPerTeam: [null, [Validators.required, Validators.min(1)]],
      maxPlayersPerTeam: [null, Validators.required],
      date: [null, Validators.required],
    });

    this.actions$.pipe(
      ofActionSuccessful(CreateTournament),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      this.dialogRef.close();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;

    const { date, ...rest } = this.form.value;
    this.store.dispatch(new CreateTournament({
      ...rest,
      date: new Date(date),
    }));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
