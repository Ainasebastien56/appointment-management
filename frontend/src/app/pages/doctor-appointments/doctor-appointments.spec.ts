import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { DoctorAppointments } from './doctor-appointments';
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
    appointmentDate: '2020-01-10',
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

function createPage(content: Appointment[]): AppointmentPage {
  return {
    content,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
  } as AppointmentPage;
}

// Rendez-vous déjà commencé (passé) et rendez-vous futur.
const PAST_SCHEDULED = createAppointment({ id: 1, appointmentDate: '2020-01-10' });
const FUTURE_SCHEDULED = createAppointment({
  id: 2,
  patientName: 'Carl Petit',
  appointmentDate: '2999-01-10',
});
const CANCELLED = createAppointment({
  id: 3,
  status: 'CANCELLED',
  cancellationReason: 'Doctor unavailable',
});

describe('DoctorAppointments', () => {
  let fixture: ComponentFixture<DoctorAppointments>;
  let component: DoctorAppointments;
  let element: HTMLElement;
  let appointmentService: {
    getAppointments: ReturnType<typeof vi.fn>;
    updateAppointmentStatus: ReturnType<typeof vi.fn>;
    updateAppointmentNotes: ReturnType<typeof vi.fn>;
  };

  async function setup(
    appointments: Appointment[],
    queryParams: Record<string, string> = {},
  ): Promise<void> {
    appointmentService = {
      getAppointments: vi.fn().mockReturnValue(of(createPage(appointments))),
      updateAppointmentStatus: vi.fn(),
      updateAppointmentNotes: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DoctorAppointments],
      providers: [
        { provide: AppointmentService, useValue: appointmentService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DoctorAppointments);
    component = fixture.componentInstance;
    element = fixture.nativeElement as HTMLElement;
    await refresh();
  }

  async function refresh(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function query(selector: string): HTMLElement | null {
    return element.querySelector(selector);
  }

  function card(id: number): HTMLElement {
    return query(`[data-testid="appointment-${id}"]`) as HTMLElement;
  }

  async function click(selector: string, root: ParentNode = element): Promise<void> {
    (root.querySelector(selector) as HTMLElement).click();
    await refresh();
  }

  async function type(selector: string, value: string): Promise<void> {
    const input = query(selector) as HTMLTextAreaElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await refresh();
  }

  it('should load today appointments by default without a sort', async () => {
    await setup([]);

    expect(appointmentService.getAppointments).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      date: getLocalDate(new Date()),
    });
    expect(element.textContent).toContain('No Appointments Today');
  });

  it('should send the filter of the selected tab and reset the page', async () => {
    await setup([]);
    component.currentPage = 2;

    await click('[data-testid="tab-PENDING_OUTCOME"]');

    expect(appointmentService.getAppointments).toHaveBeenLastCalledWith({
      page: 0,
      size: 10,
      pendingOutcomeOnly: true,
    });

    await click('[data-testid="tab-CANCELLED"]');

    expect(appointmentService.getAppointments).toHaveBeenLastCalledWith({
      page: 0,
      size: 10,
      status: 'CANCELLED',
    });
  });

  it('should open the tab given in the query params', async () => {
    await setup([], { tab: 'pending' });

    expect(component.activeTab).toBe('PENDING_OUTCOME');
    expect(appointmentService.getAppointments).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      pendingOutcomeOnly: true,
    });
  });

  it('should only offer the actions allowed for each appointment', async () => {
    await setup([PAST_SCHEDULED, FUTURE_SCHEDULED, CANCELLED]);

    const past = card(PAST_SCHEDULED.id);
    expect(past.querySelector('[data-testid="action-complete"]')).not.toBeNull();
    expect(past.querySelector('[data-testid="action-no-show"]')).not.toBeNull();
    expect(past.querySelector('[data-testid="action-cancel"]')).toBeNull();

    const future = card(FUTURE_SCHEDULED.id);
    expect(future.querySelector('[data-testid="action-complete"]')).toBeNull();
    expect(future.querySelector('[data-testid="action-no-show"]')).toBeNull();
    expect(future.querySelector('[data-testid="action-cancel"]')).not.toBeNull();

    const cancelled = card(CANCELLED.id);
    expect(cancelled.querySelector('button')).toBeNull();
    expect(cancelled.textContent).toContain('Doctor unavailable');
  });

  it('should require a reason before cancelling', async () => {
    await setup([FUTURE_SCHEDULED]);
    appointmentService.updateAppointmentStatus.mockReturnValue(
      of({ ...FUTURE_SCHEDULED, status: 'CANCELLED' }),
    );

    await click('[data-testid="action-cancel"]', card(FUTURE_SCHEDULED.id));
    const confirm = () => query('[data-testid="action-confirm"]') as HTMLButtonElement;

    expect(confirm().disabled).toBe(true);

    await type('[data-testid="action-reason"]', '   ');
    expect(confirm().disabled).toBe(true);

    await type('[data-testid="action-reason"]', '  Doctor unavailable  ');
    expect(confirm().disabled).toBe(false);

    await click('[data-testid="action-confirm"]');

    expect(appointmentService.updateAppointmentStatus).toHaveBeenCalledWith(
      FUTURE_SCHEDULED.id,
      'CANCELLED',
      'Doctor unavailable',
    );
    expect(appointmentService.getAppointments).toHaveBeenCalledTimes(2);
    expect(query('[role="dialog"]')).toBeNull();
  });

  it('should show the backend error when the status update fails', async () => {
    await setup([FUTURE_SCHEDULED]);
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    appointmentService.updateAppointmentStatus.mockReturnValue(
      throwError(() => ({
        error: { message: 'Appointments that have already started cannot be cancelled.' },
      })),
    );

    await click('[data-testid="action-cancel"]', card(FUTURE_SCHEDULED.id));
    await type('[data-testid="action-reason"]', 'Too late');
    await click('[data-testid="action-confirm"]');

    expect(query('[data-testid="action-error"]')?.textContent).toContain(
      'Appointments that have already started cannot be cancelled.',
    );
    expect(query('[role="dialog"]')).not.toBeNull();
  });

  it('should complete a started appointment without a reason', async () => {
    await setup([PAST_SCHEDULED]);
    appointmentService.updateAppointmentStatus.mockReturnValue(
      of({ ...PAST_SCHEDULED, status: 'COMPLETED' }),
    );

    await click('[data-testid="action-complete"]', card(PAST_SCHEDULED.id));
    expect(query('[data-testid="action-reason"]')).toBeNull();

    await click('[data-testid="action-confirm"]');

    expect(appointmentService.updateAppointmentStatus).toHaveBeenCalledWith(
      PAST_SCHEDULED.id,
      'COMPLETED',
      undefined,
    );
  });

  it('should save notes and update the card', async () => {
    await setup([PAST_SCHEDULED]);
    appointmentService.updateAppointmentNotes.mockReturnValue(
      of({ ...PAST_SCHEDULED, notes: 'Blood pressure normal' }),
    );

    await click('[data-testid="action-notes"]', card(PAST_SCHEDULED.id));
    const save = () => query('[data-testid="notes-save"]') as HTMLButtonElement;

    expect(save().disabled).toBe(true);

    await type('[data-testid="notes-input"]', '  Blood pressure normal ');
    expect(save().disabled).toBe(false);

    await click('[data-testid="notes-save"]');

    expect(appointmentService.updateAppointmentNotes).toHaveBeenCalledWith(
      PAST_SCHEDULED.id,
      'Blood pressure normal',
    );
    expect(query('[data-testid="notes-input"]')).toBeNull();
    expect(card(PAST_SCHEDULED.id).textContent).toContain('Blood pressure normal');
  });
});
