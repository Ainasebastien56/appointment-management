import { Component, computed, inject } from '@angular/core';

import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  template: `
    <button
      type="button"
      (click)="themeService.toggle()"
      [attr.aria-label]="label()"
      [attr.title]="label()"
      [attr.aria-pressed]="themeService.isDark()"
      class="flex h-9 w-9 items-center justify-center rounded-full border border-line bg-surface text-accent shadow-sm transition hover:bg-surface-accent"
    >
      @if (themeService.isDark()) {
        <!-- Sun: switch back to light -->
        <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8"
          stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
        </svg>
      } @else {
        <!-- Moon: switch to dark -->
        <svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" stroke="currentColor" stroke-width="1.8"
          stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
        </svg>
      }
    </button>
  `,
})
export class ThemeToggle {
  protected readonly themeService = inject(ThemeService);

  protected readonly label = computed(() =>
    this.themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode',
  );
}
