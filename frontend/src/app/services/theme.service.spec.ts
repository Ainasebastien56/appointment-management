import { TestBed } from '@angular/core/testing';

import { THEME_STORAGE_KEY, ThemeService } from './theme.service';

function mockSystemDark(matches: boolean): void {
  window.matchMedia = vi.fn().mockReturnValue({ matches } as MediaQueryList);
}

function createService(): ThemeService {
  const service = TestBed.inject(ThemeService);
  TestBed.tick(); // flush the effect that applies the theme
  return service;
}

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    mockSystemDark(false);
    TestBed.configureTestingModule({});
  });

  it('defaults to light when the system prefers light', () => {
    const service = createService();
    expect(service.theme()).toBe('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('defaults to dark when the system prefers dark', () => {
    mockSystemDark(true);
    const service = createService();
    expect(service.isDark()).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('restores the saved preference over the system one', () => {
    mockSystemDark(true);
    localStorage.setItem(THEME_STORAGE_KEY, 'light');
    expect(createService().theme()).toBe('light');
  });

  it('toggles the dark class on <html> and persists the choice', () => {
    const service = createService();

    service.toggle();
    TestBed.tick();
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');

    service.toggle();
    TestBed.tick();
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
  });
});
