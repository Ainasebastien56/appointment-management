import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

export type UserRole = 'PATIENT' | 'DOCTOR' | 'ADMIN';

export interface RegistrationRequest{
    firstName: string;
    lastName: string;
    email: string;
    dateOfBirth: string;
    gender: string;
    phone: string;
    password: string;
}

export interface RegistrationResponse{
    userId: number;
    patientId: number;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
}

@Injectable({
    providedIn: 'root',
})

export class AuthService{

    private readonly apiUrl = 'http://localhost:8080/api/auth';

    constructor(private readonly http: HttpClient) {}

    register(
        request: RegistrationRequest
    ): Observable<RegistrationResponse>{
        return this.http.post<RegistrationResponse>(
            `${this.apiUrl}/register`,
            request
        );
    }

    login(request: LoginRequest): Observable<LoginResponse> {
         return this.http.post<LoginResponse>(
            `${this.apiUrl}/login`,
            request,
    );
  }

    getRoleFromToken(token: string): UserRole | null {
    try {
      const parts = token.split('.');

      if (parts.length !== 3) {
        return null;
      }

      const payload = parts[1]
        .replace(/-/g, '+')
        .replace(/_/g, '/');

      const paddedPayload = payload.padEnd(
        payload.length + ((4 - (payload.length % 4)) % 4),
        '='
      );

      const decodedPayload = atob(paddedPayload);
      const bytes = Uint8Array.from(
        decodedPayload,
        (character) => character.charCodeAt(0)
      );

      const claims = JSON.parse(
        new TextDecoder().decode(bytes)
      ) as { role?: unknown; exp?: unknown };

      if (
        typeof claims.exp === 'number' &&
        claims.exp * 1000 <= Date.now()
      ) {
        return null;
      }

      if (
        claims.role === 'PATIENT' ||
        claims.role === 'DOCTOR' ||
        claims.role === 'ADMIN'
      ) {
        return claims.role;
      }

      return null;
    } catch {
      return null;
    }
  }

    getStoredToken(): string | null {
        return (
        localStorage.getItem('access_token') ??
        sessionStorage.getItem('access_token')
        );
    }

    getCurrentRole(): UserRole | null {
        const token = this.getStoredToken();

        return token ? this.getRoleFromToken(token) : null;
    }

    getDashboardUrl(role: UserRole): string {
        switch (role) {
        case 'PATIENT':
            return '/patient-dashboard';
        case 'DOCTOR':
            return '/doctor-dashboard';
        case 'ADMIN':
            return '/admin-dashboard';
        }
    }

    logout(): void {
        localStorage.removeItem('access_token');
        localStorage.removeItem('token_type');

        sessionStorage.removeItem('access_token');
        sessionStorage.removeItem('token_type');
    }
}