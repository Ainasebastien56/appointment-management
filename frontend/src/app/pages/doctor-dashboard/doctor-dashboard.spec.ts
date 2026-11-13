import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { DoctorDashboard } from './doctor-dashboard';
import { AppointmentService } from '../../services/appointment.service';
import { Appointment, AppointmentPage } from '../../models/appointment.model';
import { getLocalDate } from '../../utils/appointment-format';

function createAppointment(overrides: Partial<Appointment>): Appointment {
  return {
    id: 1,
    patientId: 10,
    patientName: 'Alice Martin',
    doctorId: 20,
    doctorName: 'Bob House',
    appointmentDate: '2026-11-13',
    startTime: '09:00:00',
    endTime: '09:30:00',
    type: null,
    reason: 'Checkup',
    status: 'SCHEDULED',
    notes: null,
    cancellationReason: null,
    createdAt: null,
    updatedAt: null,
    ...overrides,
  };
}

function createPage(content: Appointment[], totalElements = content.length): AppointmentPage {
  return { content, totalElements } as AppointmentPage;
}

const STATISTICS = {
  todayAppointments: 3,
  upcomingAppointments: 7,
  pendingOutcomes: 6,
  completedAppointments: 42,
};

describe('DoctorDashboard', () => {
  let fixture: ComponentFixture<DoctorDashboard>;
  let appointmentService: {
    getMyDoctorStatistics: ReturnType<typeof vi.fn>;
    getAppointments: ReturnType<typeof vi.fn>;
  };

  const todayAppointment = createAppointment({ id: 1, patientName: 'Alice Martin' });
  const pendingAppointment = createAppointment({
    id: 2,
    patientName: 'Carl Petit',
    appointmentDate: '2026-11-10',
  });

  function mockSuccess(): void {
    appointmentService.getMyDoctorStatistics.mockReturnValue(of(STATISTICS));
    appointmentService.getAppointments.mockImplementation((options) =>
      of(
        options.pendingOutcomeOnly
          ? createPage([pendingAppointment], 6)
          : createPage([todayAppointment]),
      ),
    );
  }

  async function render(): Promise<HTMLElement> {
    fixture = TestBed.createComponent(DoctorDashboard);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(async () => {
    appointmentService = {
      getMyDoctorStatistics: vi.fn(),
      getAppointments: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DoctorDashboard],
      providers: [provideRouter([]), { provide: AppointmentService, useValue: appointmentService }],
    }).compileComponents();
  });

  it('should request today and pending appointments without a sort', async () => {
    mockSuccess();

    await render();

    expect(appointmentService.getAppointments).toHaveBeenCalledWith({
      date: getLocalDate(new Date()),
      size: 100,
    });
    expect(appointmentService.getAppointments).toHaveBeenCalledWith({
      pendingOutcomeOnly: true,
      size: 5,
    });
  });

  it('should display the statistics and both lists', async () => {
    mockSuccess();

    const element = await render();
    const text = (selector: string) => element.querySelector(selector)?.textContent?.trim();

    expect(text('[data-testid="stat-today"]')).toBe('3');
    expect(text('[data-testid="stat-upcoming"]')).toBe('7');
    expect(text('[data-testid="stat-pending"]')).toBe('6');
    expect(text('[data-testid="stat-completed"]')).toBe('42');
    expect(text('[data-testid="today-list"]')).toContain('Alice Martin');
    expect(text('[data-testid="pending-list"]')).toContain('Carl Petit');
    expect(element.textContent).toContain('Showing 1 of 6.');
  });

  it('should show an error and reload on retry', async () => {
    appointmentService.getMyDoctorStatistics.mockReturnValue(throwError(() => new Error('boom')));
    appointmentService.getAppointments.mockReturnValue(of(createPage([])));
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    const element = await render();
    const alert = element.querySelector('[role="alert"]');

    expect(alert?.textContent).toContain('Unable to load your dashboard');

    mockSuccess();
    (alert?.querySelector('button') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(appointmentService.getMyDoctorStatistics).toHaveBeenCalledTimes(2);
    expect(element.querySelector('[role="alert"]')).toBeNull();
    expect(element.querySelector('[data-testid="stat-today"]')?.textContent?.trim()).toBe('3');
  });
});
