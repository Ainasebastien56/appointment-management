import { AppointmentStatus } from '../models/appointment.model';

// Helpers d'affichage partagés par les pages patient et médecin.

/**
 * Returns the display label for an appointment status.
 */
export function getStatusLabel(status: AppointmentStatus): string {
  switch (status) {
    case 'SCHEDULED':
      return 'Scheduled';
    case 'COMPLETED':
      return 'Completed';
    case 'CANCELLED':
      return 'Cancelled';
    case 'NO_SHOW':
      return 'No Show';
    default:
      return status;
  }
}

/**
 * Returns the CSS classes for an appointment status badge.
 */
export function getStatusClasses(status: AppointmentStatus): string {
  switch (status) {
    case 'SCHEDULED':
      return 'bg-surface-accent text-accent';
    case 'COMPLETED':
      return 'bg-info-soft text-info';
    case 'CANCELLED':
      return 'bg-danger-soft text-danger';
    case 'NO_SHOW':
      return 'bg-warning-soft text-warning';
    default:
      return 'bg-neutral-soft text-neutral';
  }
}

/**
 * Formats an "HH:mm" or "HH:mm:ss" time as e.g. "2:30 PM".
 */
export function formatTime(time: string): string {
  const [hours, minutes] = time.split(':').map(Number);
  const date = new Date(2000, 0, 1, hours, minutes);

  return new Intl.DateTimeFormat('en-US', {
    hour: 'numeric',
    minute: '2-digit',
  }).format(date);
}

/**
 * Parses a "YYYY-MM-DD" string as a local date (no UTC shift).
 */
export function parseLocalDate(date: string): Date {
  const [year, month, day] = date.split('-').map(Number);

  return new Date(year, month - 1, day);
}

/**
 * Formats a date as a local "YYYY-MM-DD" string.
 */
export function getLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}
