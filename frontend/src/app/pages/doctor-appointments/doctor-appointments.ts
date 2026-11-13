import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import {
  AppointmentQuery,
  AppointmentService,
  AppointmentStatusUpdate,
} from '../../services/appointment.service';
import { Appointment } from '../../models/appointment.model';
import {
  formatTime,
  getLocalDate,
  getStatusClasses,
  getStatusLabel,
  parseLocalDate,
} from '../../utils/appointment-format';

type DoctorAppointmentTab = 'TODAY' | 'UPCOMING' | 'PENDING_OUTCOME' | 'PAST' | 'CANCELLED';

interface TabDefinition {
  id: DoctorAppointmentTab;
  label: string;
  emptyTitle: string;
  emptyText: string;
}

interface PendingAction {
  appointment: Appointment;
  status: AppointmentStatusUpdate;
}

// Valeurs du query param `tab` (utilisé par le lien « Record Outcomes » du dashboard).
const TAB_BY_QUERY_PARAM: Record<string, DoctorAppointmentTab> = {
  today: 'TODAY',
  upcoming: 'UPCOMING',
  pending: 'PENDING_OUTCOME',
  past: 'PAST',
  cancelled: 'CANCELLED',
};

// Limites alignées sur la validation du backend.
export const MAX_NOTES_LENGTH = 2000;
export const MAX_REASON_LENGTH = 1000;

@Component({
  selector: 'app-doctor-appointments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './doctor-appointments.html',
  styleUrl: './doctor-appointments.css',
})
export class DoctorAppointments implements OnInit {
  private readonly appointmentService = inject(AppointmentService);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly tabs: TabDefinition[] = [
    {
      id: 'TODAY',
      label: 'Today',
      emptyTitle: 'No Appointments Today',
      emptyText: 'Your schedule is clear for today.',
    },
    {
      id: 'UPCOMING',
      label: 'Upcoming',
      emptyTitle: 'No Upcoming Appointments',
      emptyText: 'No future appointments are scheduled with you.',
    },
    {
      id: 'PENDING_OUTCOME',
      label: 'Pending Outcome',
      emptyTitle: 'All Outcomes Recorded',
      emptyText: 'Every past appointment has an outcome.',
    },
    {
      id: 'PAST',
      label: 'Past',
      emptyTitle: 'No Past Appointments',
      emptyText: 'Completed and no-show appointments will appear here.',
    },
    {
      id: 'CANCELLED',
      label: 'Cancelled',
      emptyTitle: 'No Cancelled Appointments',
      emptyText: 'Cancelled appointments will appear here.',
    },
  ];

  readonly maxNotesLength = MAX_NOTES_LENGTH;
  readonly maxReasonLength = MAX_REASON_LENGTH;

  // Data
  appointments: Appointment[] = [];
  activeTab: DoctorAppointmentTab = 'TODAY';

  // Pagination
  currentPage = 0;
  readonly pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  // UI states
  isLoading = false;
  errorMessage = '';

  // Changement de statut (Complete / No-show / Cancel)
  pendingAction: PendingAction | null = null;
  actionReason = '';
  isSubmittingAction = false;
  actionError = '';

  // Édition des notes
  editingNotesId: number | null = null;
  notesDraft = '';
  isSavingNotes = false;
  notesError = '';

  // Helpers d'affichage exposés au template
  readonly getStatusLabel = getStatusLabel;
  readonly getStatusClasses = getStatusClasses;
  readonly formatTime = formatTime;

  ngOnInit(): void {
    const tabParam = this.route.snapshot.queryParamMap.get('tab');
    this.activeTab = (tabParam && TAB_BY_QUERY_PARAM[tabParam]) || 'TODAY';

    this.loadAppointments();
  }

  get activeTabDefinition(): TabDefinition {
    return this.tabs.find((tab) => tab.id === this.activeTab) ?? this.tabs[0];
  }

  loadAppointments(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.appointmentService.getAppointments(this.buildQuery()).subscribe({
      next: (response) => {
        this.appointments = response.content ?? [];
        this.totalElements = response.totalElements ?? 0;
        this.totalPages = response.totalPages ?? 0;

        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading doctor appointments:', error);

        this.appointments = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.errorMessage =
          error.error?.message ?? 'Unable to load appointments. Please try again.';

        this.isLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  selectTab(tab: DoctorAppointmentTab): void {
    if (this.activeTab === tab) {
      return;
    }

    this.activeTab = tab;
    this.currentPage = 0;
    this.cancelNotesEdit();
    this.loadAppointments();
  }

  nextPage(): void {
    if (this.currentPage + 1 >= this.totalPages) {
      return;
    }

    this.currentPage++;
    this.loadAppointments();
  }

  previousPage(): void {
    if (this.currentPage === 0) {
      return;
    }

    this.currentPage--;
    this.loadAppointments();
  }

  /**
   * True once the appointment start time has passed (local time).
   */
  hasStarted(appointment: Appointment): boolean {
    const [hours, minutes] = appointment.startTime.split(':').map(Number);
    const start = parseLocalDate(appointment.appointmentDate);
    start.setHours(hours, minutes, 0, 0);

    return start.getTime() <= Date.now();
  }

  // Un résultat ne s'enregistre qu'une fois le rendez-vous commencé.
  canRecordOutcome(appointment: Appointment): boolean {
    return appointment.status === 'SCHEDULED' && this.hasStarted(appointment);
  }

  // Le médecin ne peut annuler qu'avant le début du rendez-vous.
  canCancel(appointment: Appointment): boolean {
    return appointment.status === 'SCHEDULED' && !this.hasStarted(appointment);
  }

  canEditNotes(appointment: Appointment): boolean {
    return appointment.status !== 'CANCELLED';
  }

  // --- Changement de statut ---

  openAction(appointment: Appointment, status: AppointmentStatusUpdate): void {
    this.pendingAction = { appointment, status };
    this.actionReason = '';
    this.actionError = '';
  }

  closeAction(): void {
    if (this.isSubmittingAction) {
      return;
    }

    this.pendingAction = null;
    this.actionReason = '';
    this.actionError = '';
  }

  get isCancellationAction(): boolean {
    return this.pendingAction?.status === 'CANCELLED';
  }

  get canConfirmAction(): boolean {
    if (!this.pendingAction || this.isSubmittingAction) {
      return false;
    }

    return !this.isCancellationAction || this.actionReason.trim().length > 0;
  }

  get actionTitle(): string {
    switch (this.pendingAction?.status) {
      case 'COMPLETED':
        return 'Mark as Completed';
      case 'NO_SHOW':
        return 'Mark as No-show';
      default:
        return 'Cancel Appointment';
    }
  }

  confirmAction(): void {
    if (!this.pendingAction || !this.canConfirmAction) {
      return;
    }

    const { appointment, status } = this.pendingAction;
    // Le motif n'est envoyé que pour une annulation, où il est obligatoire.
    const reason = this.isCancellationAction ? this.actionReason.trim() : undefined;

    this.isSubmittingAction = true;
    this.actionError = '';

    this.appointmentService.updateAppointmentStatus(appointment.id, status, reason).subscribe({
      next: () => {
        this.isSubmittingAction = false;
        this.pendingAction = null;
        this.actionReason = '';

        // Le rendez-vous change d'onglet : on recharge la liste.
        this.loadAppointments();
      },
      error: (error) => {
        console.error('Error updating appointment status:', error);

        this.actionError =
          error.error?.message ?? 'Unable to update this appointment. Please try again.';
        this.isSubmittingAction = false;
        this.cdr.markForCheck();
      },
    });
  }

  // --- Notes ---

  startNotesEdit(appointment: Appointment): void {
    if (!this.canEditNotes(appointment)) {
      return;
    }

    this.editingNotesId = appointment.id;
    this.notesDraft = appointment.notes ?? '';
    this.notesError = '';
  }

  cancelNotesEdit(): void {
    if (this.isSavingNotes) {
      return;
    }

    this.editingNotesId = null;
    this.notesDraft = '';
    this.notesError = '';
  }

  get canSaveNotes(): boolean {
    const trimmed = this.notesDraft.trim();

    return !this.isSavingNotes && trimmed.length > 0 && trimmed.length <= MAX_NOTES_LENGTH;
  }

  saveNotes(appointment: Appointment): void {
    if (!this.canSaveNotes) {
      return;
    }

    this.isSavingNotes = true;
    this.notesError = '';

    this.appointmentService
      .updateAppointmentNotes(appointment.id, this.notesDraft.trim())
      .subscribe({
        next: (updated) => {
          this.appointments = this.appointments.map((item) =>
            item.id === updated.id ? updated : item,
          );

          this.isSavingNotes = false;
          this.editingNotesId = null;
          this.notesDraft = '';
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Error saving appointment notes:', error);

          this.notesError = error.error?.message ?? 'Unable to save the notes. Please try again.';
          this.isSavingNotes = false;
          this.cdr.markForCheck();
        },
      });
  }

  formatAppointmentDate(date: string): string {
    return new Intl.DateTimeFormat('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    }).format(parseLocalDate(date));
  }

  // Pas de `sort` : le backend applique le tri par défaut du médecin.
  private buildQuery(): AppointmentQuery {
    const query: AppointmentQuery = {
      page: this.currentPage,
      size: this.pageSize,
    };

    switch (this.activeTab) {
      case 'TODAY':
        query.date = getLocalDate(new Date());
        break;
      case 'UPCOMING':
        query.upcomingOnly = true;
        break;
      case 'PENDING_OUTCOME':
        query.pendingOutcomeOnly = true;
        break;
      case 'PAST':
        query.pastOutcomeOnly = true;
        break;
      case 'CANCELLED':
        query.status = 'CANCELLED';
        break;
    }

    return query;
  }
}
