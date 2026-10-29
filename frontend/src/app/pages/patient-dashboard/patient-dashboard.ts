
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AppointmentService } from '../../services/appointment.service';
import {
  Appointment,
  AppointmentStatus,
} from '../../models/appointment.model';

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
  calendarDays: CalendarDay[] = [];

  isLoading = true;
  errorMessage = '';

  readonly today = this.getLocalDate(new Date());

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

    this.appointmentService.getAppointments(0, 100).subscribe({
      next: (page) => {
        this.appointments = page.content ?? [];
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
    return this.appointments.length;
  }

  get upcomingAppointments(): Appointment[] {
    return this.appointments
      .filter(
        (appointment) =>
          appointment.status === 'SCHEDULED' &&
          this.getAppointmentDateTime(appointment) >= new Date(),
      )
      .sort((a, b) =>
        this.getAppointmentDateTime(a).getTime() -
        this.getAppointmentDateTime(b).getTime(),
      );
  }

  get upcomingCount(): number {
    return this.upcomingAppointments.length;
  }

  get completedCount(): number {
    return this.appointments.filter(
      (appointment) => appointment.status === 'COMPLETED',
    ).length;
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
    }).format(this.parseLocalDate(this.selectedDate));
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

  getStatusLabel(status: AppointmentStatus): string {
    switch (status) {
      case 'SCHEDULED':
        return 'Scheduled';
      case 'COMPLETED':
        return 'Completed';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status;
    }
  }

  getStatusClasses(status: AppointmentStatus): string {
    switch (status) {
      case 'SCHEDULED':
        return 'bg-[#E8F0E7] text-[#245640]';
      case 'COMPLETED':
        return 'bg-blue-50 text-blue-700';
      case 'CANCELLED':
        return 'bg-red-50 text-red-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  }

  formatAppointmentDate(date: string): string {
    return new Intl.DateTimeFormat('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    }).format(this.parseLocalDate(date));
  }

  formatTime(time: string): string {
    const [hours, minutes] = time.split(':').map(Number);
    const date = new Date(2000, 0, 1, hours, minutes);

    return new Intl.DateTimeFormat('en-US', {
      hour: 'numeric',
      minute: '2-digit',
    }).format(date);
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

      const dateString = this.getLocalDate(date);
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

  private getAppointmentDateTime(appointment: Appointment): Date {
    const [year, month, day] = appointment.appointmentDate
      .split('-')
      .map(Number);

    const [hours, minutes, seconds = 0] = appointment.startTime
      .split(':')
      .map(Number);

    return new Date(year, month - 1, day, hours, minutes, seconds);
  }

  private parseLocalDate(date: string): Date {
    const [year, month, day] = date.split('-').map(Number);

    return new Date(year, month - 1, day);
  }

  private getLocalDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  
  parseAppointmentMonth(date: string): string {
    return new Intl.DateTimeFormat('en-US', {
      month: 'short',
    }).format(this.parseLocalDate(date));
  }

}
