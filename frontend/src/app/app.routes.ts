import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';

export const routes: Routes = [
    {
        path:'',
        pathMatch: 'full',
        loadComponent:() =>
            import('./pages/login/login').then((m) => m.Login),
    },
    {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register').then((m) => m.Register),
    },
  // Shared layout for all patient pages
  {
    path: '',
    canActivate: [authGuard],
    data: { role: 'PATIENT' },
    loadComponent: () =>
      import('./layouts/patient-layout/patient-layout')
        .then((m) => m.PatientLayout),
    children: [
      {
        path: 'patient-dashboard',
        loadComponent: () =>
          import('./pages/patient-dashboard/patient-dashboard')
            .then((m) => m.PatientDashboard),
      },
      {
        path: 'book-appointment',
        loadComponent: () =>
          import('./pages/book-appointment/book-appointment')
            .then((m) => m.BookAppointment),
      },
      ],
    },
    {
    path: 'book-appointment',
    canActivate: [authGuard],
    data: { role: 'PATIENT' },
    loadComponent: () =>
        import('./pages/book-appointment/book-appointment')
        .then((m) => m.BookAppointment),
    },
    {
    path: 'doctor-dashboard',
    canActivate: [authGuard],
    data: { role: 'DOCTOR'},
    loadComponent: () =>
      import('./pages/doctor-dashboard/doctor-dashboard')
        .then((m) => m.DoctorDashboard),
    },
    {
    path: 'admin-dashboard',
    canActivate: [authGuard],
    data: { role: 'ADMIN'},
    loadComponent: () =>
      import('./pages/admin-dashboard/admin-dashboard')
        .then((m) => m.AdminDashboard),
    },
    {
        path: '**',
        redirectTo: '',
    }
    
];
