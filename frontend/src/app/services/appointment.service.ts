import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppointmentPage } from '../models/appointment.model';

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

export interface AppointmentResponse {
  id: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  type: string;
  reason: string;
  status: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentStatistics {
  scheduledAppointments: number;
  pendingOutcomes: number;
  cancelledAppointments: number;
}


@Injectable({
  providedIn: 'root',
})
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/appointments';

  getAvailableSlots(
    date: string,
    doctorId?: number,
  ): Observable<AvailableSlot[]> {
    let params = new HttpParams().set('date', date);

    if (doctorId !== undefined) {
      params = params.set('doctorId', doctorId);
    }

    return this.http.get<AvailableSlot[]>(
      `${this.apiUrl}/available-slots`,
      { params },
    );
  }

  createAppointment(
    request: AppointmentRequest
  ): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(this.apiUrl, request);
  }

  cancelAppointment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getAppointments(
    page = 0,
    size = 100,
    status?: string,
    upcomingOnly = false,
    pendingOutcomeOnly = false,
    pastOutcomeOnly = false,
    doctorName?: string): Observable<AppointmentPage> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'appointmentDate,asc');

    if (status) {
      params = params.set('status', status);
    }

    if (doctorName?.trim()) {
      params = params.set('doctorName', doctorName.trim());
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

    return this.http.get<AppointmentPage>(
      this.apiUrl,
      { params }
    );
  }

  getMyAppointmentStatistics(): Observable<AppointmentStatistics> {
    return this.http.get<AppointmentStatistics>(
      `${this.apiUrl}/my-statistics`
    );
  }
}
