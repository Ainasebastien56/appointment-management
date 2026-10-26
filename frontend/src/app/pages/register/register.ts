
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectorRef,
  Component,
  inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  AuthService,
  RegistrationRequest,
} from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  protected showPassword = false;
  protected showConfirmPassword = false;
  protected isSubmitting = false;
  protected registrationSucceeded = false;

  protected error = '';
  protected notice = '';

  protected readonly currentYear = new Date().getFullYear();

  // La date de naissance doit être antérieure à aujourd'hui.
  protected readonly maxBirthDate = (() => {
    const date = new Date();
    date.setDate(date.getDate() - 1);

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  })();

  protected form = {
    firstName: '',
    lastName: '',
    email: '',
    dateOfBirth: '',
    gender: 'MALE',
    phone: '',
    password: '',
    confirmPassword: '',
  };

  protected togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  protected toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  protected onSubmit(): void {
    this.error = '';
    this.notice = '';

    if (this.isSubmitting || this.registrationSucceeded) {
      return;
    }

    if (this.form.password !== this.form.confirmPassword) {
      this.error = 'Passwords do not match.';
      this.cdr.markForCheck();
      return;
    }

    const request: RegistrationRequest = {
      firstName: this.form.firstName.trim(),
      lastName: this.form.lastName.trim(),
      email: this.form.email.trim(),
      dateOfBirth: this.form.dateOfBirth,
      gender: this.form.gender,
      phone: this.form.phone.trim(),
      password: this.form.password,
    };

    this.isSubmitting = true;
    this.cdr.markForCheck();

    this.authService.register(request).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.registrationSucceeded = true;

        this.form.password = '';
        this.form.confirmPassword = '';

        this.notice =
          'Your account has been created successfully. You can now sign in.';

        this.cdr.markForCheck();
      },

      error: (httpError: HttpErrorResponse) => {
        this.isSubmitting = false;
        this.error = this.getErrorMessage(httpError);

        this.cdr.markForCheck();
      },
    });
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    const apiMessage = error.error?.message;

    if (typeof apiMessage === 'string' && apiMessage.trim()) {
      return apiMessage;
    }

    if (error.status === 0) {
      return 'Unable to connect to the server. Please try again later.';
    }

    if (error.status === 400) {
      return 'Please check your information and try again.';
    }

    if (error.status === 409) {
      return 'An account with this email address may already exist.';
    }

    return 'We could not create your account. Please try again.';
  }
}
