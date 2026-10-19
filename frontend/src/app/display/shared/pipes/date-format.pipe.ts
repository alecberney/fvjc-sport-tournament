import { formatDate } from '@angular/common';
import { inject, LOCALE_ID, Pipe, PipeTransform } from '@angular/core';

export type DateFormat = 'date' | 'time';

// Single place owning the app's date patterns, so templates never inline one.
@Pipe({
  name: 'dateFormat',
  standalone: true,
})
export class DateFormatPipe implements PipeTransform {
  private static readonly PATTERNS: Record<DateFormat, string> = {
    date: 'dd MMM yyyy',
    time: 'HH:mm',
  };

  private readonly locale = inject(LOCALE_ID);

  transform(value: Date | null | undefined, format: DateFormat): string {
    if (!value) return '';
    return formatDate(value, DateFormatPipe.PATTERNS[format], this.locale);
  }
}
