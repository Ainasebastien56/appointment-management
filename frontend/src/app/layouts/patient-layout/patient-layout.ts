import { Component, inject } from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';

import { ThemeToggle } from '../../components/theme-toggle/theme-toggle';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-patient-layout',
  standalone:true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ThemeToggle],
  templateUrl: './patient-layout.html',
  styleUrl: './patient-layout.css',
})
export class PatientLayout {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  logout() : void{
    this.authService.logout();
    void this.router.navigate(['/'])
  }

}
