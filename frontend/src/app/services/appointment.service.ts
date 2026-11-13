import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Appointment, AppointmentPage, AppointmentStatus } from '../models/appointment.model';

export interface AvailableSlot {
  doctorId: number;
  doctorName: string;
  startTime: string;
  endTime: string;
}

export interface AppointmentRequest {
  doctorId?: number;
  appointmentDate: string;
  startTime: string;
  reason: string;
  notes?: string;
  type?: string;
}

export interface AppointmentStatistics {
  scheduledAppointments: number;
  pendingOutcomes: number;
  cancelledAppointments: number;
}

export interface DoctorAppointmentStatistics {
  todayAppointments: number;
  upcomingAppointments: number;
  pendingOutcomes: number;
  completedAppointments: number;
}

// Filtres de la liste des rendez-vous. Les filtres acceptés dépendent du rôle côté backend.
export interface AppointmentQuery {
  page?: number;
  size?: number;
  status?: AppointmentStatus;
  upcomingOnly?: boolean;
  pendingOutcomeOnly?: boolean;
  pastOutcomeOnly?: boolean;
  doctorName?: string;
  // Format YYYY-MM-DD.
  date?: string;
  // Envoyé seulement s'il est fourni, pour laisser le backend appliquer son tri par défaut.
  sort?: string;
}

// Statuts qu'un médecin peut appliquer via PATCH /{id}/status.
export type AppointmentStatusUpdate = Exclude<AppointmentStatus, 'SCHEDULED'>;

@Injectable({
  providedIn: 'root',
})
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/appointments';

  getAvailableSlots(date: string, doctorId?: number): Observable<AvailableSlot[]> {
    let params = new HttpParams().set('date', date);

    if (doctorId !== undefined) {
      params = params.set('doctorId', doctorId);
    }

    return this.http.get<AvailableSlot[]>(`${this.apiUrl}/available-slots`, {
      params,
    });
  }

  createAppointment(request: AppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(this.apiUrl, request);
  }

  cancelAppointment(id: number, reason?: string): Observable<void> {
    const options = reason !== undefined ? { body: { reason } } : {};

    return this.http.delete<void>(`${this.apiUrl}/${id}`, options);
  }

  getAppointments(options: AppointmentQuery = {}): Observable<AppointmentPage> {
    const {
      page = 0,
      size = 100,
      status,
      upcomingOnly = false,
      pendingOutcomeOnly = false,
      pastOutcomeOnly = false,
      doctorName,
      date,
      sort,
    } = options;

    let params = new HttpParams().set('page', page).set('size', size);

    if (sort) {
      params = params.set('sort', sort);
    }

    if (status) {
      params = params.set('status', status);
    }

    if (doctorName?.trim()) {
      params = params.set('doctorName', doctorName.trim());
    }

    if (date) {
      params = params.set('date', date);
    }

    if (upcomingOnly) {
      params = params.set('upcomingOnly', true);
    }

    if (pendingOutcomeOnly) {
      params = params.set('pendingOutcomeOnly', true);
    }

    if (pastOutcomeOnly) {
      params = params.set('pastOutcomeOnly', true);
    }

    return this.http.get<AppointmentPage>(this.apiUrl, { params });
  }

  updateAppointmentStatus(
    id: number,
    status: AppointmentStatusUpdate,
    changeReason?: string,
  ): Observable<Appointment> {
    const body = changeReason !== undefined ? { status, changeReason } : { status };

    return this.http.patch<Appointment>(`${this.apiUrl}/${id}/status`, body);
  }

  updateAppointmentNotes(id: number, notes: string): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.apiUrl}/${id}/notes`, {
      notes,
    });
  }

  getMyAppointmentStatistics(): Observable<AppointmentStatistics> {
    return this.http.get<AppointmentStatistics>(`${this.apiUrl}/my-statistics`);
  }

  getMyDoctorStatistics(): Observable<DoctorAppointmentStatistics> {
    return this.http.get<DoctorAppointmentStatistics>(`${this.apiUrl}/my-doctor-statistics`);
  }
}
