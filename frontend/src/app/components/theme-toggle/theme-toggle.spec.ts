import { TestBed } from '@angular/core/testing';

import { ThemeService } from '../../services/theme.service';
import { ThemeToggle } from './theme-toggle';

describe('ThemeToggle', () => {
  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    window.matchMedia = vi.fn().mockReturnValue({ matches: false } as MediaQueryList);
    await TestBed.configureTestingModule({ imports: [ThemeToggle] }).compileComponents();
  });

  it('switches theme and updates its accessible label on click', async () => {
    const fixture = TestBed.createComponent(ThemeToggle);
    await fixture.whenStable();
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;

    expect(button.getAttribute('aria-label')).toBe('Switch to dark mode');
    expect(button.getAttribute('aria-pressed')).toBe('false');

    button.click();
    await fixture.whenStable();

    expect(TestBed.inject(ThemeService).isDark()).toBe(true);
    expect(button.getAttribute('aria-label')).toBe('Switch to light mode');
    expect(button.getAttribute('aria-pressed')).toBe('true');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });
});
