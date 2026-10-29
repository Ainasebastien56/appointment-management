import { Component, OnInit, inject, ChangeDetectorRef} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AppointmentService, AvailableSlot } from '../../services/appointment.service';
import { Doctor, DoctorService } from '../../services/doctor.service';

@Component({
  selector: 'app-book-appointment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './book-appointment.html',
  styleUrl: './book-appointment.css',
})
export class BookAppointment implements OnInit {
  private readonly doctorService = inject(DoctorService);
  private readonly appointmentService = inject(AppointmentService);
  private readonly cdr = inject(ChangeDetectorRef);


  doctors: Doctor[] = [];
  slots: AvailableSlot[] = [];

  selectedDoctorId: number | null = null;
  selectedDate = this.getLocalDate()
  selectedSlot : AvailableSlot | null = null;
  reason = '';
  notes = '';

  isLoadingDoctors = false;
  isLoadingSlots = false;
  isSubmitting = false;

  errorMessage = '';
  successMessage = '';

  readonly minDate = this.getLocalDate();

  ngOnInit(): void {
    this.loadDoctors();
  }

  private getLocalDate():string{
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  loadDoctors(): void {
    this.isLoadingDoctors = true;
    this.errorMessage = '';

    this.doctorService.getDoctors().subscribe({
      next: (doctors) =>{
        this.doctors = doctors;
        this.isLoadingDoctors = false;

        this.cdr.markForCheck();

        this.loadSlots();
        
      },
      error: () =>{
        this.isLoadingDoctors = false;
        this.errorMessage = 'Unable to load doctors. Please try again.';

        this.cdr.markForCheck();
      }
    });

  }

    onCriteriaChange(): void {
    this.selectedSlot = null;
    this.successMessage = '';
    this.loadSlots();
  }

loadSlots(): void {
  if (!this.selectedDate) {
    this.slots = [];
    this.selectedSlot = null;
    this.isLoadingSlots = false;
    return;
  }

  if (this.selectedDate < this.minDate) {
    this.slots = [];
    this.selectedSlot = null;
    this.isLoadingSlots = false;
    this.errorMessage = 'Please select a valid appointment date.';
    return;
  }

  this.isLoadingSlots = true;
  this.errorMessage = '';
  this.selectedSlot = null;

  const doctorId = this.selectedDoctorId ?? undefined;

  this.appointmentService
    .getAvailableSlots(this.selectedDate, doctorId)
    .subscribe({
      next: (slots) => {
        this.slots = slots;
        this.isLoadingSlots = false;

        this.cdr.markForCheck();
      },
      error: (error) => {
        this.slots = [];
        this.isLoadingSlots = false;
        this.errorMessage =
          'Unable to load available time slots. Please try again.';

        this.cdr.markForCheck();
      },
    });
}

   selectSlot(slot: AvailableSlot): void {
    this.selectedSlot = slot;
    this.errorMessage = '';
    this.successMessage = '';
  }

    submitAppointment(): void {
    if (
      !this.selectedDate ||
      !this.selectedSlot ||
      !this.reason.trim()
    ) {
      this.errorMessage =
        'Please select a time slot and enter the reason for your visit.';
      return;
    }

    if (this.selectedDate < this.minDate) {
      this.errorMessage =
        'The appointment date cannot be in the past.';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.appointmentService
      .createAppointment({
        doctorId: this.selectedSlot.doctorId,
        appointmentDate: this.selectedDate,
        startTime: this.selectedSlot.startTime,
        reason: this.reason.trim(),
        notes: this.notes.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.isSubmitting = false;
          this.successMessage =
            'Your appointment has been booked successfully.';

          this.reason = '';
          this.notes = '';
          this.selectedSlot = null;

          this.loadSlots();
        },
        error: (error: HttpErrorResponse) => {
          this.isSubmitting = false;

          if (error.status === 401 || error.status === 403) {
            this.errorMessage =
              'Your session has expired or you are not authorized to perform this action.';
          } else if (
            error.status === 400 ||
            error.status === 409
          ) {
            this.errorMessage =
              error.error?.message ??
              'This time slot is no longer available or the submitted information is invalid. Please select another time slot.';

            this.loadSlots();
          } else {
            this.errorMessage =
              'Booking failed. Please try again.';
          }
        },
      });
  }

}
