import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

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
}