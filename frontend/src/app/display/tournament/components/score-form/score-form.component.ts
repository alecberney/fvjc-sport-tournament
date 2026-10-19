import { Component, EventEmitter, inject, Input, OnChanges, Output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface ScoreFormValue {
  score1: number;
  score2: number;
}

@Component({
  selector: 'app-score-form',
  templateUrl: './score-form.component.html',
  styleUrl: './score-form.component.scss',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
})
export class ScoreFormComponent implements OnChanges {

  private readonly fb = inject(FormBuilder);

  @Input({ required: true }) team1Label!: string;
  @Input({ required: true }) team2Label!: string;
  @Input() score1: number | null = null;
  @Input() score2: number | null = null;
  @Output() submitted = new EventEmitter<ScoreFormValue>();

  readonly form = this.fb.group({
    score1: this.fb.control<number | null>(null, [Validators.required, Validators.min(0), Validators.max(500)]),
    score2: this.fb.control<number | null>(null, [Validators.required, Validators.min(0), Validators.max(500)]),
  });

  // Any input change means another match (or a refreshed result) is shown:
  // re-seed the form with its scores so the user never edits stale values.
  ngOnChanges(): void {
    this.form.reset({ score1: this.score1, score2: this.score2 });
  }

  submit(): void {
    const { score1, score2 } = this.form.getRawValue();
    if (this.form.invalid || score1 === null || score2 === null) return;
    this.submitted.emit({ score1, score2 });
  }
}
