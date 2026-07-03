import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { RegisterTeams } from '@app/domain/team/team.actions';

@Component({
  selector: 'app-team-register-modal',
  templateUrl: './team-register.modal.html',
  styleUrl: './team-register.modal.scss',
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
export class TeamRegisterModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TeamRegisterModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;

  get paidArray(): FormArray {
    return this.form.get('paid') as FormArray;
  }

  get paidControls() {
    return this.paidArray.controls;
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(250)]],
      responsibleFirstName: ['', [Validators.required, Validators.maxLength(100)]],
      responsibleLastName: ['', [Validators.required, Validators.maxLength(100)]],
      count: [1, [Validators.required, Validators.min(1)]],
      paid: this.fb.array([this.fb.control(false)]),
    });

    this.form.get('count')!.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(count => {
      this.syncPaidArray(Number(count) || 1);
    });

    this.actions$.pipe(
      ofActionSuccessful(RegisterTeams),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;
    const { name, responsibleFirstName, responsibleLastName, count, paid } = this.form.value;
    this.store.dispatch(new RegisterTeams(this.data.tournamentId, {
      name,
      responsibleFirstName,
      responsibleLastName,
      count: Number(count),
      paid: paid as boolean[],
    }));
  }

  cancel(): void {
    this.dialogRef.close();
  }

  teamLabel(index: number): string {
    const name = this.form.get('name')?.value || '—';
    return this.form.get('count')?.value === 1 ? name : `${name} ${index + 1}`;
  }

  private syncPaidArray(count: number): void {
    while (this.paidArray.length < count) {
      this.paidArray.push(this.fb.control(false));
    }
    while (this.paidArray.length > count) {
      this.paidArray.removeAt(this.paidArray.length - 1);
    }
  }
}
