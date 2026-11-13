import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { PatientLayout } from './patient-layout';

describe('PatientLayout', () => {
  let component: PatientLayout;
  let fixture: ComponentFixture<PatientLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientLayout],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
