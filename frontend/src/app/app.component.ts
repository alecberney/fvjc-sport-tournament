import { Component, inject, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {

  private readonly http = inject(HttpClient);

  message = '';

  ngOnInit(): void {
    this.http.get<{ message: string }>('/api/hello').subscribe({
      next: (res) => (this.message = res.message),
      error: () => (this.message = 'Could not reach backend'),
    });
  }
}
