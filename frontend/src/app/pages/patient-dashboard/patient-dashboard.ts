
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AppointmentService } from '../../services/appointment.service';
import { Appointment } from '../../models/appointment.model';
import {
  formatTime,
  getLocalDate,
  getStatusClasses,
  getStatusLabel,
  parseLocalDate,
} from '../../utils/appointment-format';

// Le patient voit ses rendez-vous dans l'ordre chronologique.
const PATIENT_SORT = 'appointmentDate,asc';

interface CalendarDay {
  date: string;
  dayNumber: number;
  isCurrentMonth: boolean;
  isToday: boolean;
  hasAppointments: boolean;
  appointmentCount: number;
}

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './patient-dashboard.html',
})
export class PatientDashboard implements OnInit {
  private readonly appointmentService = inject(AppointmentService);
  private readonly cdr = inject(ChangeDetectorRef);

  appointments: Appointment[] = [];
  upcomingAppointmentsList: Appointment[] = [];
  calendarDays: CalendarDay[] = [];
  totalAppointmentsCount = 0;
  completedAppointmentsCount = 0;
  upcomingAppointmentsCount = 0;

  isLoading = true;
  errorMessage = '';

  readonly today = getLocalDate(new Date());

  // Helpers d'affichage exposés au template
  readonly getStatusLabel = getStatusLabel;
  readonly getStatusClasses = getStatusClasses;
  readonly formatTime = formatTime;

  currentMonth = new Date(
    new Date().getFullYear(),
    new Date().getMonth(),
    1,
  );

  selectedDate: string | null = null;

  ngOnInit(): void {
    this.buildCalendar();
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.isLoading = true;
    this.errorMessage = '';

    forkJoin({
      appointmentsPage: this.appointmentService.getAppointments({
        page: 0,
        size: 100,
        sort: PATIENT_SORT,
      }),
      completedPage: this.appointmentService.getAppointments({
        page: 0,
        size: 1,
        status: 'COMPLETED',
        sort: PATIENT_SORT,
      }),
      upcomingPage: this.appointmentService.getAppointments({
        page: 0,
        size: 5,
        upcomingOnly: true,
        sort: PATIENT_SORT,
      }),
    }).subscribe({
      next: ({ appointmentsPage, completedPage, upcomingPage }) => {
        this.appointments = appointmentsPage.content ?? [];

        this.totalAppointmentsCount =
          appointmentsPage.totalElements ?? 0;

        this.completedAppointmentsCount =
          completedPage.totalElements ?? 0;

        this.upcomingAppointmentsCount =
          upcomingPage.totalElements ?? 0;

        this.upcomingAppointmentsList =
          upcomingPage.content ?? [];

        this.buildCalendar();

        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Unable to load appointments:', error);

        this.errorMessage =
          'Unable to load your appointments. Please try again.';

        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get totalAppointments(): number {
    return this.totalAppointmentsCount;
  }

  get upcomingAppointments(): Appointment[] {
    return this.upcomingAppointmentsList;
  }

  get upcomingCount(): number {
    return this.upcomingAppointmentsCount;
  }

  get completedCount(): number {
    return this.completedAppointmentsCount;
  }

  get displayedAppointments(): Appointment[] {
    if (this.selectedDate) {
      return this.appointments
        .filter(
          (appointment) =>
            appointment.appointmentDate === this.selectedDate,
        )
        .sort((a, b) => a.startTime.localeCompare(b.startTime));
    }

    return this.upcomingAppointments.slice(0, 5);
  }

  get monthLabel(): string {
    return new Intl.DateTimeFormat('en-US', {
      month: 'long',
      year: 'numeric',
    }).format(this.currentMonth);
  }

  get selectedDateLabel(): string {
    if (!this.selectedDate) {
      return 'Upcoming Appointments';
    }

    return new Intl.DateTimeFormat('en-US', {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    }).format(parseLocalDate(this.selectedDate));
  }

  get hasAppointmentsOnSelectedDate(): boolean {
    return this.displayedAppointments.length > 0;
  }

  previousMonth(): void {
    this.currentMonth = new Date(
      this.currentMonth.getFullYear(),
      this.currentMonth.getMonth() - 1,
      1,
    );

    this.buildCalendar();
  }

  nextMonth(): void {
    this.currentMonth = new Date(
      this.currentMonth.getFullYear(),
      this.currentMonth.getMonth() + 1,
      1,
    );

    this.buildCalendar();
  }

  selectCalendarDay(day: CalendarDay): void {
    this.selectedDate = day.date;
  }

  clearDateFilter(): void {
    this.selectedDate = null;
  }

  formatAppointmentDate(date: string): string {
    return new Intl.DateTimeFormat('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    }).format(parseLocalDate(date));
  }

  private buildCalendar(): void {
    const year = this.currentMonth.getFullYear();
    const month = this.currentMonth.getMonth();

    const firstDay = new Date(year, month, 1);
    const mondayBasedOffset = (firstDay.getDay() + 6) % 7;

    const calendarStart = new Date(year, month, 1 - mondayBasedOffset);

    const appointmentCounts = new Map<string, number>();

    for (const appointment of this.appointments) {
      if (appointment.status === 'CANCELLED') {
        continue;
      }

      const date = appointment.appointmentDate;

      appointmentCounts.set(
        date,
        (appointmentCounts.get(date) ?? 0) + 1,
      );
    }

    this.calendarDays = Array.from({ length: 42 }, (_, index) => {
      const date = new Date(
        calendarStart.getFullYear(),
        calendarStart.getMonth(),
        calendarStart.getDate() + index,
      );

      const dateString = getLocalDate(date);
      const appointmentCount = appointmentCounts.get(dateString) ?? 0;

      return {
        date: dateString,
        dayNumber: date.getDate(),
        isCurrentMonth: date.getMonth() === month,
        isToday: dateString === this.today,
        hasAppointments: appointmentCount > 0,
        appointmentCount,
      };
    });
  }

  parseAppointmentMonth(date: string): string {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
    }).format(parseLocalDate(date));
  }

}
