import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

export interface Doctor{
    id: number;
    firstName: string;
    lastName: string;
    specialty: string;
}

@Injectable({
    providedIn: 'root',
})

export class DoctorService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = 'http://localhost:8080/api/doctors';

    getDoctors(): Observable<Doctor[]> {
        return this.http.get<Doctor[]>(this.apiUrl);
    }
}