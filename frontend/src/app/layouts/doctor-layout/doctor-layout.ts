import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ThemeToggle } from '../../components/theme-toggle/theme-toggle';
import { AuthService } from '../../services/auth.service';

// Layout partagé par toutes les pages médecin (sidebar, header, déconnexion)
@Component({
  selector: 'app-doctor-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ThemeToggle],
  templateUrl: './doctor-layout.html',
  styleUrl: './doctor-layout.css',
})
export class DoctorLayout {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
  }
}
