
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  protected showPassword = false;
  protected rememberMe = false;
  protected isSubmitting = false;

  protected notice = '';
  protected error = '';

  protected readonly currentYear = new Date().getFullYear();

  protected form = {
    email: '',
    password: '',
  };

  protected togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  protected onRememberMeChange(event: Event): void {
    this.rememberMe = (event.target as HTMLInputElement).checked;
  }

  protected onSubmit(): void {
    this.notice = '';
    this.error = '';

    if (this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;

    this.authService.login({
      email: this.form.email.trim(),
      password: this.form.password,
    }).subscribe({
      next: (response) => {
        if (!response.accessToken) {
          this.isSubmitting = false;
          this.error = 'The server did not return an access token.';

          this.cdr.markForCheck();
          return;
        }

        const storage = this.rememberMe
          ? localStorage
          : sessionStorage;

        const otherStorage = this.rememberMe
          ? sessionStorage
          : localStorage;

        // Conserver le jeton selon le choix de l'utilisateur.
        otherStorage.removeItem('access_token');
        otherStorage.removeItem('token_type');

        storage.setItem('access_token', response.accessToken);
        storage.setItem('token_type', response.tokenType || 'Bearer');

        this.isSubmitting = false;
        this.notice = 'Signed in successfully.';
        this.form.password = '';

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

    if (error.status === 401) {
      return 'Invalid email address or password.';
    }

    if (error.status === 400) {
      return 'Please check your email address and password.';
    }

    return 'Sign-in failed. Please try again later.';
  }
}
