import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import {
  AppointmentService,
  DoctorAppointmentStatistics,
} from '../../services/appointment.service';
import { Appointment } from '../../models/appointment.model';
import {
  formatTime,
  getLocalDate,
  getStatusClasses,
  getStatusLabel,
  parseLocalDate,
} from '../../utils/appointment-format';

// Nombre de rendez-vous affichés dans la liste « Needs outcome ».
const PENDING_PREVIEW_SIZE = 5;

@Component({
  selector: 'app-doctor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './doctor-dashboard.html',
  styleUrl: './doctor-dashboard.css',
})
export class DoctorDashboard implements OnInit {
  private readonly appointmentService = inject(AppointmentService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly today = getLocalDate(new Date());
  readonly pendingPreviewSize = PENDING_PREVIEW_SIZE;

  statistics: DoctorAppointmentStatistics | null = null;
  todayAppointments: Appointment[] = [];
  pendingAppointments: Appointment[] = [];
  pendingTotal = 0;

  isLoading = true;
  errorMessage = '';

  // Helpers d'affichage exposés au template
  readonly getStatusLabel = getStatusLabel;
  readonly getStatusClasses = getStatusClasses;
  readonly formatTime = formatTime;

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.isLoading = true;
    this.errorMessage = '';

    // Pas de `sort` : le backend applique le tri par défaut du médecin
    // (heure croissante pour la date du jour, plus récent d'abord pour le reste).
    forkJoin({
      statistics: this.appointmentService.getMyDoctorStatistics(),
      todayPage: this.appointmentService.getAppointments({
        date: this.today,
        size: 100,
      }),
      pendingPage: this.appointmentService.getAppointments({
        pendingOutcomeOnly: true,
        size: PENDING_PREVIEW_SIZE,
      }),
    }).subscribe({
      next: ({ statistics, todayPage, pendingPage }) => {
        this.statistics = statistics;
        this.todayAppointments = todayPage.content ?? [];
        this.pendingAppointments = pendingPage.content ?? [];
        this.pendingTotal = pendingPage.totalElements ?? 0;

        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Unable to load doctor dashboard:', error);

        this.errorMessage = 'Unable to load your dashboard. Please try again.';
        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get todayLabel(): string {
    return new Intl.DateTimeFormat('en-US', {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
    }).format(parseLocalDate(this.today));
  }

  formatAppointmentDate(date: string): string {
    return new Intl.DateTimeFormat('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    }).format(parseLocalDate(date));
  }
}
