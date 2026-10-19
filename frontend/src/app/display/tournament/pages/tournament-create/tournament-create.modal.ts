import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Sport } from '@app/domain/tournament/tournament.model';
import { CreateTournament } from '@app/domain/tournament/tournament.actions';

@Component({
  selector: 'app-tournament-create-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
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
export class TournamentCreateModal implements OnInit {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TournamentCreateModal>);
  private readonly formBuilder = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly destroyRef = inject(DestroyRef);

  readonly sports = Object.values(Sport);

  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(250)]],
    sport: this.formBuilder.control<Sport | null>(null, Validators.required),
    numberOfFields: this.formBuilder.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(500)]),
    minPlayersPerTeam: this.formBuilder.control<number | null>(null, [Validators.required, Validators.min(1)]),
    maxPlayersPerTeam: this.formBuilder.control<number | null>(null, Validators.required),
    date: this.formBuilder.control<Date | null>(null, Validators.required),
  });

  ngOnInit(): void {
    this.actions$.pipe(
      ofActionSuccessful(CreateTournament),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => {
      this.dialogRef.close();
    });
  }

  submit(): void {
    if (this.form.invalid) return;

    const { name, sport, numberOfFields, minPlayersPerTeam, maxPlayersPerTeam, date } = this.form.getRawValue();
    this.store.dispatch(new CreateTournament({
      name: name!,
      sport: sport!,
      numberOfFields: numberOfFields!,
      minPlayersPerTeam: minPlayersPerTeam!,
      maxPlayersPerTeam: maxPlayersPerTeam!,
      date: new Date(date!),
    }));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
