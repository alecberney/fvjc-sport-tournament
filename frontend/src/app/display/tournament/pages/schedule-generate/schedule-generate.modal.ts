import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { GenerateSchedule } from '@app/domain/schedule/schedule.actions';

@Component({
  selector: 'app-schedule-generate-modal',
  templateUrl: './schedule-generate.modal.html',
  styleUrl: './schedule-generate.modal.scss',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
})
export class ScheduleGenerateModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<ScheduleGenerateModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      startTime: ['09:00', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
      matchDurationMinutes: [20, [Validators.required, Validators.min(1)]],
      breakDurationMinutes: [5, [Validators.required, Validators.min(0)]],
    });

    this.actions$.pipe(
      ofActionSuccessful(GenerateSchedule),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;
    this.store.dispatch(new GenerateSchedule(
      this.data.tournamentId,
      String(this.form.value.startTime),
      Number(this.form.value.matchDurationMinutes),
      Number(this.form.value.breakDurationMinutes),
    ));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
