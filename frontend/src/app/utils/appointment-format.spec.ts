import {
  formatTime,
  getLocalDate,
  getStatusClasses,
  getStatusLabel,
  parseLocalDate,
} from './appointment-format';

describe('appointment-format', () => {
  it('should return a label for every status', () => {
    expect(getStatusLabel('SCHEDULED')).toBe('Scheduled');
    expect(getStatusLabel('COMPLETED')).toBe('Completed');
    expect(getStatusLabel('CANCELLED')).toBe('Cancelled');
    expect(getStatusLabel('NO_SHOW')).toBe('No Show');
  });

  it('should return badge classes for every status', () => {
    expect(getStatusClasses('SCHEDULED')).toBe('bg-surface-accent text-accent');
    expect(getStatusClasses('COMPLETED')).toBe('bg-info-soft text-info');
    expect(getStatusClasses('CANCELLED')).toBe('bg-danger-soft text-danger');
    expect(getStatusClasses('NO_SHOW')).toBe('bg-warning-soft text-warning');
  });

  it('should format a time in 12-hour format', () => {
    expect(formatTime('14:30:00')).toBe('2:30 PM');
    expect(formatTime('09:00')).toBe('9:00 AM');
  });

  it('should convert local dates to and from YYYY-MM-DD', () => {
    const date = parseLocalDate('2026-03-05');

    expect(date.getFullYear()).toBe(2026);
    expect(date.getMonth()).toBe(2);
    expect(date.getDate()).toBe(5);
    expect(getLocalDate(date)).toBe('2026-03-05');
  });
});
