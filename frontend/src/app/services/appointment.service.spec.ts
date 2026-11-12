import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AppointmentService } from './appointment.service';

const API_URL = 'http://localhost:8080/api/appointments';

describe('AppointmentService', () => {
  let service: AppointmentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AppointmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getAppointments', () => {
    it('sends only the default page and size when called without options', () => {
      service.getAppointments().subscribe();

      const req = httpMock.expectOne((r) => r.url === API_URL);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys().sort()).toEqual(['page', 'size']);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('100');
      req.flush({ content: [] });
    });

    it('sends every given option as a query param', () => {
      service
        .getAppointments({
          page: 2,
          size: 10,
          status: 'CANCELLED',
          upcomingOnly: true,
          pendingOutcomeOnly: true,
          pastOutcomeOnly: true,
          doctorName: '  Smith  ',
          date: '2026-11-12',
          sort: 'appointmentDate,asc',
        })
        .subscribe();

      const req = httpMock.expectOne((r) => r.url === API_URL);
      const params = req.request.params;
      expect(params.get('page')).toBe('2');
      expect(params.get('size')).toBe('10');
      expect(params.get('status')).toBe('CANCELLED');
      expect(params.get('upcomingOnly')).toBe('true');
      expect(params.get('pendingOutcomeOnly')).toBe('true');
      expect(params.get('pastOutcomeOnly')).toBe('true');
      expect(params.get('doctorName')).toBe('Smith');
      expect(params.get('date')).toBe('2026-11-12');
      expect(params.get('sort')).toBe('appointmentDate,asc');
      req.flush({ content: [] });
    });

    it('omits false flags and a blank doctor name', () => {
      service
        .getAppointments({
          upcomingOnly: false,
          pendingOutcomeOnly: false,
          pastOutcomeOnly: false,
          doctorName: '   ',
        })
        .subscribe();

      const req = httpMock.expectOne((r) => r.url === API_URL);
      expect(req.request.params.keys().sort()).toEqual(['page', 'size']);
      req.flush({ content: [] });
    });
  });

  describe('updateAppointmentStatus', () => {
    it('sends a PATCH with the status and the change reason', () => {
      service.updateAppointmentStatus(7, 'CANCELLED', 'Doctor unavailable').subscribe();

      const req = httpMock.expectOne(`${API_URL}/7/status`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({
        status: 'CANCELLED',
        changeReason: 'Doctor unavailable',
      });
      req.flush({});
    });

    it('sends only the status when no reason is given', () => {
      service.updateAppointmentStatus(7, 'COMPLETED').subscribe();

      const req = httpMock.expectOne(`${API_URL}/7/status`);
      expect(req.request.body).toEqual({ status: 'COMPLETED' });
      req.flush({});
    });
  });

  it('updateAppointmentNotes sends a PATCH with the notes', () => {
    service.updateAppointmentNotes(3, 'Follow-up in 2 weeks').subscribe();

    const req = httpMock.expectOne(`${API_URL}/3/notes`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ notes: 'Follow-up in 2 weeks' });
    req.flush({});
  });

  it('getMyDoctorStatistics calls the doctor statistics endpoint', () => {
    service.getMyDoctorStatistics().subscribe();

    const req = httpMock.expectOne(`${API_URL}/my-doctor-statistics`);
    expect(req.request.method).toBe('GET');
    req.flush({
      todayAppointments: 1,
      upcomingAppointments: 2,
      pendingOutcomes: 3,
      completedAppointments: 4,
    });
  });

  describe('cancelAppointment', () => {
    it('sends a DELETE without a body when no reason is given', () => {
      service.cancelAppointment(5).subscribe();

      const req = httpMock.expectOne(`${API_URL}/5`);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.body).toBeNull();
      req.flush(null);
    });

    it('sends the reason in the DELETE body when given', () => {
      service.cancelAppointment(5, 'Feeling better').subscribe();

      const req = httpMock.expectOne(`${API_URL}/5`);
      expect(req.request.method).toBe('DELETE');
      expect(req.request.body).toEqual({ reason: 'Feeling better' });
      req.flush(null);
    });
  });
});
