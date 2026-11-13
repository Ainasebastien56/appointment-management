import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { DoctorLayout } from './doctor-layout';
import { AuthService } from '../../services/auth.service';

describe('DoctorLayout', () => {
  let component: DoctorLayout;
  let fixture: ComponentFixture<DoctorLayout>;
  let authService: { logout: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authService = { logout: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [DoctorLayout],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DoctorLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show the doctor navigation links', () => {
    const links: HTMLAnchorElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('nav a'),
    );
    const hrefs = links.map((link) => link.getAttribute('href'));

    expect(hrefs).toContain('/doctor-dashboard');
    expect(hrefs).toContain('/doctor-appointments');
    expect(fixture.nativeElement.textContent).toContain('Doctor Portal');
  });

  it('should log out and navigate to the login page', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });
});
