import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');
  // Created at startup so the theme class and saved preference stay in sync on every route.
  private readonly theme = inject(ThemeService);
}
