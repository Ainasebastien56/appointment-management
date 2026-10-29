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
  doctorId: number;
  appointmentDate: string;
  startTime: string;
  reason: string;
  notes?: string;
  type?: string;
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
  ): Observable<AvailableSlot[]>{
    let params = new HttpParams().set('date', date);

    if (doctorId !== undefined){
      params = params.set('doctorId', doctorId);
    }

    return this.http.get<AvailableSlot[]>(
      `${this.apiUrl}/available-slots`,
      {params},
    );
  }

  createAppointment(
    request: AppointmentRequest
  ): Observable<unknown> {
    return this.http.post(this.apiUrl, request);
  }

  getAppointments(
    page =0 ,
    size = 100): Observable<AppointmentPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'appointmentDate,asc');

    return this.http.get<AppointmentPage>(
      this.apiUrl, 
      { params }
    );
  }
}
