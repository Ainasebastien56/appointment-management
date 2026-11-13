import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { PatientDashboard } from './patient-dashboard';

describe('PatientDashboard', () => {
  let component: PatientDashboard;
  let fixture: ComponentFixture<PatientDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientDashboard],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
