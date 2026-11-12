import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AppointmentService, AppointmentStatistics } from '../../services/appointment.service';
import { Appointment, AppointmentStatus } from '../../models/appointment.model';

type AppointmentTab =
  | 'UPCOMING'
  | 'PAST'
  | 'PENDING_OUTCOME'
  | 'CANCELLED';

// Le patient voit ses rendez-vous dans l'ordre chronologique.
const PATIENT_SORT = 'appointmentDate,asc';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-appointments.html',
  styleUrl: './my-appointments.css',
})
export class MyAppointments implements OnInit {
  private readonly appointmentService = inject(AppointmentService);
  private readonly cdr = inject(ChangeDetectorRef);

  // Data
  appointments: Appointment[] = [];

  // Appointment statistics
  statistics: AppointmentStatistics | null = null;
  isStatisticsLoading = false;
  statisticsError = '';

  // Tabs
  activeTab: AppointmentTab = 'UPCOMING';

  // Pagination
  currentPage = 0;
  readonly pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  // Filters
  searchTerm = '';

  // UI states
  isLoading = false;
  errorMessage = '';

  // Cancellation
  appointmentToCancel: Appointment | null = null;
  isCancelling = false;
  cancellationError = '';

  ngOnInit(): void {
    this.loadAppointments();
    this.loadAppointmentStatistics();
  }

  /**
   * Loads appointments according to the active tab.
   */
  loadAppointments(): void {
    this.isLoading = true;
    this.errorMessage = '';

    // Determine the temporal filter according to the active tab.
    const upcomingOnly = this.activeTab === 'UPCOMING';

    const pendingOutcomeOnly = this.activeTab === 'PENDING_OUTCOME';

    const pastOutcomeOnly = this.activeTab === 'PAST';

    this.appointmentService.getAppointments({
      page: this.currentPage,
      size: this.pageSize,
      status: this.activeTab === 'CANCELLED' ? 'CANCELLED' : undefined,
      upcomingOnly,
      pendingOutcomeOnly,
      pastOutcomeOnly,
      doctorName: this.searchTerm,
      sort: PATIENT_SORT,
    }).subscribe({
      next: (response) => {
        this.appointments = response.content;

        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;

        this.isLoading = false;
        this.cdr.markForCheck();
      },

      error: (error) => {
        console.error('Error loading appointments:', error);

        this.appointments = [];
        this.totalElements = 0;
        this.totalPages = 0;

        this.errorMessage = error.error?.message ?? 'Unable to load appointments. Please try again.'

        this.isLoading = false;
        this.cdr.markForCheck();
      }
    })

  }

  /**
 * Loads all appointment statistics for the authenticated patient.
 */
  loadAppointmentStatistics(): void {
    this.isStatisticsLoading = true;
    this.statisticsError = '';

    this.appointmentService.getMyAppointmentStatistics().subscribe({
      next: (response) => {
        this.statistics = response;

        this.isStatisticsLoading = false;
        this.cdr.markForCheck();
      },

      error: (error) => {
        console.error('Error loading appointment statistics:', error);

        this.statistics = null;

        this.statisticsError =
          error.error?.message ??
          'Unable to load appointment statistics. Please try again.';

        this.isStatisticsLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  /**
 * Changes the active appointment category.
 */
  selectTab(tab: AppointmentTab): void {
    if (this.activeTab === tab) {
      return;
    }
    this.activeTab = tab;
    this.currentPage = 0;
    this.searchTerm = '';

    this.loadAppointments();
  }

  /**
 * Applies the doctor name search.
 */
  onSearchChange(value: string): void {
    this.searchTerm = value;
    this.currentPage = 0;
    this.loadAppointments();
  }

  /**
 * Moves to the next page.
 */
  nextPage(): void {
    if (this.currentPage + 1 >= this.totalPages) {
      return;
    }

    this.currentPage++;
    this.loadAppointments();
  }

  /**
 * Moves to the previous page.
 */
  previousPage(): void {
    if (this.currentPage === 0) {
      return;
    }

    this.currentPage--;
    this.loadAppointments();
  }

  /**
   * Returns the display label for an appointment status.
   */
  getStatusLabel(status: AppointmentStatus): string {
    switch (status) {
      case 'SCHEDULED':
        return 'Scheduled';
      case 'COMPLETED':
        return 'Completed';
      case 'NO_SHOW':
        return 'No Show';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status;
    }
  }

  /**
 * Returns the CSS classes for an appointment status.
 */
  getStatusClasses(status: AppointmentStatus): string {
    switch (status) {
      case 'SCHEDULED':
        return 'bg-surface-accent text-accent';
      case 'COMPLETED':
        return 'bg-info-soft text-info';
      case 'NO_SHOW':
        return 'bg-warning-soft text-warning';
      case 'CANCELLED':
        return 'bg-danger-soft text-danger';
      default:
        return 'bg-neutral-soft text-neutral';
    }
  }

  /**
 * Resets all filters.
 */
  clearFilters(): void {
    this.searchTerm = '';
    this.currentPage = 0;

    this.loadAppointments();
  }

  /**
 * Opens the cancellation confirmation.
 */
  confirmCancellation(appointment: Appointment): void {
    if (appointment.status !== 'SCHEDULED') {
      return;
    }

    this.appointmentToCancel = appointment;
    this.cancellationError = '';
  }

  /**
   * Closes the cancellation confirmation.
   */
  closeCancellationDialog(): void {
    if (this.isCancelling) {
      return;
    }

    this.appointmentToCancel = null;
    this.cancellationError = '';
  }

  /**
   * Cancels the selected appointment.
   */
  cancelAppointment(): void {
    if (!this.appointmentToCancel || this.isCancelling) {
      return;
    }

    const appointmentId = this.appointmentToCancel.id;

    this.isCancelling = true;
    this.cancellationError = '';

    this.appointmentService.cancelAppointment(appointmentId).subscribe({
      next: () => {
        this.appointmentToCancel = null;
        this.isCancelling = false;

        // Refresh the current appointment list.
        this.loadAppointments();

        // Refresh statistics after cancellation.
        this.loadAppointmentStatistics();
      },

      error: (error) => {
        console.error('Error cancelling appointment:', error);

        this.cancellationError =
          error.error?.message ??
          'Unable to cancel this appointment. Please try again.';

        this.isCancelling = false;
        this.cdr.markForCheck();
      }
    });
  }

}
