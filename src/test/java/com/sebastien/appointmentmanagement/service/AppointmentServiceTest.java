
package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.entity.*;
import com.sebastien.appointmentmanagement.exception.AppointmentNotFoundException;
import com.sebastien.appointmentmanagement.repository.AppointmentRepository;
import com.sebastien.appointmentmanagement.repository.DoctorRepository;
import com.sebastien.appointmentmanagement.repository.DoctorScheduleExceptionRepository;
import com.sebastien.appointmentmanagement.repository.DoctorScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {

    private AppointmentRepository createAppointmentRepository() {
        return mock(AppointmentRepository.class);
    }

    private DoctorRepository createDoctorRepository() {
        return mock(DoctorRepository.class);
    }


    private AppointmentService createService(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository) {

        return createService(
                appointmentRepository,
                doctorRepository,
                mock(DoctorScheduleRepository.class),
                mock(DoctorScheduleExceptionRepository.class)
        );
    }

    private AppointmentService createService(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            DoctorScheduleRepository scheduleRepository) {

        return createService(
                appointmentRepository,
                doctorRepository,
                scheduleRepository,
                mock(DoctorScheduleExceptionRepository.class)
        );
    }

    private AppointmentService createService(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            DoctorScheduleRepository scheduleRepository,
            DoctorScheduleExceptionRepository exceptionRepository) {

        return new AppointmentService(
                appointmentRepository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );
    }

    private Patient createPatient() {
        User user = new User(
                "jean.dupont@example.com",
                "test-password",
                UserRole.PATIENT
        );

        return new Patient(
                user,
                "Jean",
                "Dupont",
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "555-0100"
        );
    }

    private Doctor createDoctor(String firstName, String lastName) {
        User user = new User(
                firstName.toLowerCase() + "." +
                        lastName.toLowerCase() + "@example.com",
                "test-password",
                UserRole.DOCTOR
        );

        return new Doctor(
                user,
                firstName,
                lastName,
                "General Practice",
                "LIC-" + lastName.toUpperCase(),
                "555-0200",
                LocalDate.of(2020, 1, 1)
        );
    }

    private Appointment createAppointment(
            Patient patient,
            Doctor doctor,
            LocalDate date,
            AppointmentStatus status) {

        Appointment appointment = new Appointment(
                patient,
                doctor,
                date,
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                "Routine check-up",
                status
        );

        appointment.setType(AppointmentType.CONSULTATION);
        return appointment;
    }

    private AppointmentRequest createRequest(
            Long doctorId,
            LocalDate date,
            LocalTime startTime) {

        AppointmentRequest request = new AppointmentRequest();
        request.setDoctorId(doctorId);
        request.setAppointmentDate(date);
        request.setStartTime(startTime);
        request.setType(AppointmentType.CONSULTATION);
        request.setReason("Routine check-up");
        request.setNotes("Test notes");

        return request;
    }

    @Test
    void shouldReturnAppointmentById() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        Appointment appointment = createAppointment(
                patient,
                doctor,
                LocalDate.of(2026, 10, 20),
                AppointmentStatus.SCHEDULED
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.getAppointmentById(1L);

        assertEquals("Jean Dupont", response.getPatientName());
        assertEquals("Dr Martin", response.getDoctorName());
        assertEquals(
                LocalDate.of(2026, 10, 20),
                response.getAppointmentDate()
        );
        assertEquals(LocalTime.of(9, 0), response.getStartTime());
        assertEquals(LocalTime.of(9, 30), response.getEndTime());
        assertEquals(
                AppointmentStatus.SCHEDULED,
                response.getStatus()
        );

        verify(repository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenAppointmentDoesNotExist() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        AppointmentNotFoundException exception = assertThrows(
                AppointmentNotFoundException.class,
                () -> service.getAppointmentById(999L)
        );

        assertEquals(
                "No appointment found with id: 999",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateAppointmentForProvidedPatient() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        DoctorScheduleRepository scheduleRepository = mock(DoctorScheduleRepository.class);

        AppointmentService service =
                createService(repository, doctorRepository, scheduleRepository);

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

// Choisir une date future pour que le test reste valide dans le temps.
        AppointmentRequest request = createRequest(
                10L,
                LocalDate.now().plusDays(7),
                LocalTime.of(10, 0)
        );

        LocalDate appointmentDate = request.getAppointmentDate();

        DoctorSchedule schedule = new DoctorSchedule(
                doctor,
                appointmentDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                10L,
                appointmentDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        when(doctorRepository.findById(10L))
                .thenReturn(Optional.of(doctor));

        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response =
                service.createAppointment(request, patient);

        assertEquals("Jean Dupont", response.getPatientName());
        assertEquals("Dr Martin", response.getDoctorName());
        assertEquals(
                LocalDate.now().plusDays(7),
                response.getAppointmentDate()
        );
        assertEquals(LocalTime.of(10, 0), response.getStartTime());
        assertEquals(LocalTime.of(10, 30), response.getEndTime());
        assertEquals(
                AppointmentStatus.SCHEDULED,
                response.getStatus()
        );
        assertEquals(
                AppointmentType.CONSULTATION,
                response.getType()
        );
        assertEquals("Routine check-up", response.getReason());
        assertEquals("Test notes", response.getNotes());

        verify(doctorRepository).findById(10L);
        verify(repository).save(any(Appointment.class));
    }


    @Test
    void shouldRejectAppointmentOutsideDoctorWorkingHours() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);

        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository
        );

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        LocalDate appointmentDate = LocalDate.now().plusDays(7);

        AppointmentRequest request = createRequest(
                10L,
                appointmentDate,
                LocalTime.of(16, 45)
        );

        DoctorSchedule schedule = new DoctorSchedule(
                doctor,
                appointmentDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        when(doctorRepository.findById(10L))
                .thenReturn(Optional.of(doctor));

        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                10L,
                appointmentDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createAppointment(request, patient)
        );

        verify(repository, never()).save(any(Appointment.class));
    }

    @Test
    void shouldRejectAppointmentOnDoctorDayOff(){
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        DoctorScheduleRepository scheduleRepository = mock(DoctorScheduleRepository.class);

        DoctorScheduleExceptionRepository exceptionRepository = mock(DoctorScheduleExceptionRepository.class);

        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        LocalDate appointmentDate = LocalDate.now().plusDays(7);

        AppointmentRequest request = createRequest(
                10L,
                appointmentDate,
                LocalTime.of(10, 0)
        );

        DoctorScheduleException dayOff =
                mock(DoctorScheduleException.class);

        when(dayOff.getType())
                .thenReturn(ScheduleExceptionType.DAY_OFF);

        when(doctorRepository.findById(10L))
                .thenReturn(Optional.of(doctor));


        when(exceptionRepository.findByDoctor_IdAndDate(
                10L,
                appointmentDate
        )).thenReturn(List.of(dayOff));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createAppointment(request, patient)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "The doctor is not available on the requested date.",
                exception.getReason()
        );

        verify(repository, never()).save(any(Appointment.class));

        verify(scheduleRepository, never())
                .findByDoctor_IdAndDayOfWeek(
                        10L,
                        appointmentDate.getDayOfWeek()
                );
    }

    @Test
    void shouldUseCustomHoursInsteadOfRegularSchedule() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);

        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        LocalDate appointmentDate = LocalDate.now().plusDays(7);

        AppointmentRequest request = createRequest(
                10L,
                appointmentDate,
                LocalTime.of(14, 0)
        );

        DoctorScheduleException customHours =
                mock(DoctorScheduleException.class);

        when(customHours.getType())
                .thenReturn(ScheduleExceptionType.CUSTOM_HOURS);

        when(customHours.getStartTime())
                .thenReturn(LocalTime.of(13, 0));

        when(customHours.getEndTime())
                .thenReturn(LocalTime.of(15, 0));

        when(doctorRepository.findById(10L))
                .thenReturn(Optional.of(doctor));

        when(exceptionRepository.findByDoctor_IdAndDate(
                10L,
                appointmentDate
        )).thenReturn(List.of(customHours));

        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response =
                service.createAppointment(request, patient);

        assertEquals(LocalTime.of(14, 0), response.getStartTime());
        assertEquals(LocalTime.of(14, 30), response.getEndTime());

        verify(repository).save(any(Appointment.class));

        verify(scheduleRepository, never())
                .findByDoctor_IdAndDayOfWeek(
                        10L,
                        appointmentDate.getDayOfWeek()
                );
    }

    @Test
    void shouldRejectAppointmentOutsideCustomHours() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);

        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        LocalDate appointmentDate = LocalDate.now().plusDays(7);

        AppointmentRequest request = createRequest(
                10L,
                appointmentDate,
                LocalTime.of(10, 0)
        );

        DoctorScheduleException customHours =
                mock(DoctorScheduleException.class);

        when(customHours.getType())
                .thenReturn(ScheduleExceptionType.CUSTOM_HOURS);

        when(customHours.getStartTime())
                .thenReturn(LocalTime.of(13, 0));

        when(customHours.getEndTime())
                .thenReturn(LocalTime.of(15, 0));

        when(doctorRepository.findById(10L))
                .thenReturn(Optional.of(doctor));

        when(exceptionRepository.findByDoctor_IdAndDate(
                10L,
                appointmentDate
        )).thenReturn(List.of(customHours));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createAppointment(request, patient)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "The requested time is outside the doctor's exceptional working hours.",
                exception.getReason()
        );

        verify(repository, never()).save(any(Appointment.class));

        verify(scheduleRepository, never())
                .findByDoctor_IdAndDayOfWeek(
                        10L,
                        appointmentDate.getDayOfWeek()
                );
    }


    @Test
    void shouldRejectAppointmentWhenDoctorHasConflictingAppointment() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);

        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        LocalDate appointmentDate = LocalDate.now().plusDays(7);

        AppointmentRequest request = createRequest(
                10L,
                appointmentDate,
                LocalTime.of(10, 0)
        );

        DoctorSchedule schedule = new DoctorSchedule(
                doctor,
                appointmentDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        when(doctorRepository.findById(10L))
                .thenReturn(Optional.of(doctor));

        when(exceptionRepository.findByDoctor_IdAndDate(
                10L,
                appointmentDate
        )).thenReturn(List.of());

        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                10L,
                appointmentDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        when(repository
                .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        10L,
                        appointmentDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0)
                ))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createAppointment(request, patient)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "The doctor already has an appointment during the requested time.",
                exception.getReason()
        );

        verify(repository, never()).save(any(Appointment.class));
    }

    @Test
    void shouldRejectAppointmentWhenPatientHasConflictingAppointment() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);

        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        LocalDate appointmentDate = LocalDate.now().plusDays(7);

        AppointmentRequest request = createRequest(
                10L,
                appointmentDate,
                LocalTime.of(10, 0)
        );

        DoctorSchedule schedule = new DoctorSchedule(
                doctor,
                appointmentDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        when(doctorRepository.findById(10L))
                .thenReturn(Optional.of(doctor));

        when(exceptionRepository.findByDoctor_IdAndDate(
                10L,
                appointmentDate
        )).thenReturn(List.of());

        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                10L,
                appointmentDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        when(repository
                .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        10L,
                        appointmentDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0)
                ))
                .thenReturn(false);

        when(repository
                .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        patient,
                        appointmentDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0)
                ))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createAppointment(request, patient)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "The patient already has an appointment during the requested time.",
                exception.getReason()
        );

        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldRejectAppointmentNotAlignedWithThirtyMinuteSlot() {
        // Arrange : créer les repositories simulés et le service.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);

        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        // Créer le patient et le médecin concernés.
        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        // Choisir une date future et demander un début à 10 h 15.
        LocalDate appointmentDate = LocalDate.now().plusDays(7);

        AppointmentRequest request = createRequest(
                10L,
                appointmentDate,
                LocalTime.of(10, 15)
        );

        // Définir les horaires habituels du médecin de 09 h à 17 h.
        DoctorSchedule schedule = new DoctorSchedule(
                doctor,
                appointmentDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        // Simuler la recherche du médecin et l'absence d'exception.
        when(doctorRepository.findById(10L))
                .thenReturn(Optional.of(doctor));

        when(exceptionRepository.findByDoctor_IdAndDate(
                10L,
                appointmentDate
        )).thenReturn(List.of());

        // Le créneau entre dans les horaires de travail,
        // mais son heure de début n'est pas alignée sur 30 minutes.
        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                10L,
                appointmentDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        // Act et Assert : le service doit refuser la réservation.
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createAppointment(request, patient)
        );

        // Vérifier qu'aucun rendez-vous n'a été enregistré.
        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldAllowAppointmentWhenNoScheduledConflictExists() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);

        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        LocalDate appointmentDate = LocalDate.now().plusDays(7);

        AppointmentRequest request = createRequest(
                10L,
                appointmentDate,
                LocalTime.of(10, 0)
        );

        DoctorSchedule schedule = new DoctorSchedule(
                doctor,
                appointmentDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        when(doctorRepository.findById(10L))
                .thenReturn(Optional.of(doctor));

        when(exceptionRepository.findByDoctor_IdAndDate(
                10L,
                appointmentDate
        )).thenReturn(List.of());

        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                10L,
                appointmentDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        when(repository
                .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        10L,
                        appointmentDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0)
                ))
                .thenReturn(false);

        when(repository
                .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        patient,
                        appointmentDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0)
                ))
                .thenReturn(false);

        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response =
                service.createAppointment(request, patient);

        assertEquals(
                AppointmentStatus.SCHEDULED,
                response.getStatus()
        );

        verify(repository).existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                10L,
                appointmentDate,
                AppointmentStatus.SCHEDULED,
                LocalTime.of(10, 30),
                LocalTime.of(10, 0)
        );

        verify(repository).existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                patient,
                appointmentDate,
                AppointmentStatus.SCHEDULED,
                LocalTime.of(10, 30),
                LocalTime.of(10, 0)
        );

        verify(repository).save(any(Appointment.class));
    }

    @Test
    void shouldRejectCreationWithoutAuthenticatedPatient() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        AppointmentRequest request = createRequest(
                10L,
                LocalDate.of(2026, 10, 20),
                LocalTime.of(10, 0)
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.createAppointment(request)
        );

        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldAllowPatientToCancelOwnAppointmentMoreThanTwoHoursBefore() {
        // Arrange : créer les repositories simulés et le service à tester.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        // Le service utilise ici des mocks des repositories de planning,
        // car ce test porte uniquement sur l'annulation.
        AppointmentService service =
                createService(repository, doctorRepository);

        // Simuler un patient déjà enregistré avec l'identifiant 1.
        // Cet identifiant permettra de vérifier qu'il annule son propre rendez-vous.
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(1L);

        // Créer le médecin associé au rendez-vous.
        Doctor doctor = createDoctor("Dr", "Martin");

        // Programmer le rendez-vous trois heures dans le futur.
        // On retire les secondes et les nanosecondes pour rendre
        // les valeurs plus faciles à lire et à vérifier.
        LocalDateTime appointmentStart =
                LocalDateTime.now().plusHours(3).withSecond(0).withNano(0);

        // Créer un rendez-vous dont le statut initial est SCHEDULED.
        Appointment appointment = new Appointment(
                patient,
                doctor,
                appointmentStart.toLocalDate(),
                appointmentStart.toLocalTime(),
                appointmentStart.toLocalTime().plusMinutes(30),
                "Routine check-up",
                AppointmentStatus.SCHEDULED
        );

        // Simuler la recherche du rendez-vous portant l'identifiant 1.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Simuler la sauvegarde : Mockito renvoie le même objet
        // après la modification de son statut.
        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act : demander au service d'annuler le rendez-vous
        // au nom du patient authentifié.
        AppointmentResponse response = service.cancelAppointment(
                1L,
                UserRole.PATIENT,
                patient
        );

        // Assert : vérifier que le rendez-vous est maintenant annulé.
        assertEquals(
                AppointmentStatus.CANCELLED,
                response.getStatus()
        );

        // Vérifier que le rendez-vous a été sauvegardé,
        // et non supprimé de la base de données.
        verify(repository).save(appointment);
    }


    @Test
    void shouldRejectPatientCancellationWithinTwoHours() {
        // Arrange : créer les repositories simulés et le service.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        // Simuler le patient authentifié avec un identifiant existant.
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(1L);

        // Créer le médecin associé au rendez-vous.
        Doctor doctor = createDoctor("Dr", "Martin");

        // Prévoir le rendez-vous dans 90 minutes :
        // il se trouve donc à l'intérieur de la limite des deux heures.
        LocalDateTime appointmentStart =
                LocalDateTime.now().plusMinutes(90)
                        .withSecond(0)
                        .withNano(0);

        // Créer un rendez-vous encore planifié.
        Appointment appointment = new Appointment(
                patient,
                doctor,
                appointmentStart.toLocalDate(),
                appointmentStart.toLocalTime(),
                appointmentStart.toLocalTime().plusMinutes(30),
                "Routine check-up",
                AppointmentStatus.SCHEDULED
        );

        // Simuler la recherche du rendez-vous par son identifiant.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Act et Assert : l'annulation doit être refusée
        // car le rendez-vous commence dans moins de deux heures.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelAppointment(
                        1L,
                        UserRole.PATIENT,
                        patient
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        assertEquals(
                "Patients must cancel at least 2 hours before the appointment.",
                exception.getReason()
        );

        // Vérifier que le rendez-vous n'a pas été sauvegardé :
        // son statut doit rester SCHEDULED.
        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldAllowAdminToCancelAppointmentWithinTwoHours() {
        // Arrange : créer les repositories simulés et le service.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        // Créer les personnes associées au rendez-vous.
        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        // Programmer le rendez-vous dans 90 minutes :
        // le patient serait soumis à la restriction des deux heures.
        LocalDateTime appointmentStart =
                LocalDateTime.now().plusMinutes(90)
                        .withSecond(0)
                        .withNano(0);

        // Créer un rendez-vous dont le statut initial est SCHEDULED.
        Appointment appointment = new Appointment(
                patient,
                doctor,
                appointmentStart.toLocalDate(),
                appointmentStart.toLocalTime(),
                appointmentStart.toLocalTime().plusMinutes(30),
                "Routine check-up",
                AppointmentStatus.SCHEDULED
        );

        // Simuler la récupération du rendez-vous par son identifiant.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Simuler la sauvegarde du rendez-vous après son annulation.
        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act : l'administrateur annule le rendez-vous.
        // Le profil patient est null car l'administrateur n'annule
        // pas au nom d'un patient authentifié.
        AppointmentResponse response = service.cancelAppointment(
                1L,
                UserRole.ADMIN,
                null
        );

        // Assert : vérifier que le rendez-vous est bien annulé.
        assertEquals(
                AppointmentStatus.CANCELLED,
                response.getStatus()
        );

        // Vérifier que le rendez-vous est sauvegardé,
        // et non supprimé de la base de données.
        verify(repository).save(appointment);
    }


    @Test
    void shouldRejectPatientCancellingAnotherPatientsAppointment() {
        // Arrange : préparer les repositories simulés et le service.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        // Simuler le véritable propriétaire du rendez-vous (patient ID 2).
        Patient appointmentOwner = mock(Patient.class);
        when(appointmentOwner.getId()).thenReturn(2L);

        // Simuler le patient qui tente d'annuler le rendez-vous (patient ID 1).
        Patient requestingPatient = mock(Patient.class);
        when(requestingPatient.getId()).thenReturn(1L);

        // Créer le médecin du rendez-vous.
        Doctor doctor = createDoctor("Dr", "Martin");

        // Prévoir le rendez-vous suffisamment à l'avance pour que
        // le délai des deux heures ne soit pas la cause du refus.
        LocalDateTime appointmentStart =
                LocalDateTime.now().plusHours(3)
                        .withSecond(0)
                        .withNano(0);

        // Créer un rendez-vous planifié appartenant au patient ID 2.
        Appointment appointment = new Appointment(
                appointmentOwner,
                doctor,
                appointmentStart.toLocalDate(),
                appointmentStart.toLocalTime(),
                appointmentStart.toLocalTime().plusMinutes(30),
                "Routine check-up",
                AppointmentStatus.SCHEDULED
        );

        // Simuler la recherche du rendez-vous dans le repository.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Act et Assert : le patient ID 1 ne peut pas annuler
        // le rendez-vous qui appartient au patient ID 2.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelAppointment(
                        1L,
                        UserRole.PATIENT,
                        requestingPatient
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        assertEquals(
                "Patients can only cancel their own appointments.",
                exception.getReason()
        );

        // Vérifier que le rendez-vous n'a pas été modifié ni sauvegardé.
        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldRejectCancellationOfCompletedAppointment() {
        // Arrange : créer les repositories simulés et le service.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        // Créer un rendez-vous dont le statut est déjà COMPLETED.
        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.now().plusDays(1),
                AppointmentStatus.COMPLETED
        );

        // Simuler la recherche du rendez-vous par son identifiant.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Act et Assert : même l'administrateur ne peut pas annuler
        // un rendez-vous déjà terminé.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelAppointment(
                        1L,
                        UserRole.ADMIN,
                        null
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        assertEquals(
                "Only scheduled appointments can be cancelled.",
                exception.getReason()
        );

        // Vérifier que le rendez-vous n'a pas été sauvegardé.
        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldRejectCancellationOfAlreadyCancelledAppointment() {
        // Arrange : créer les repositories simulés et le service.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        // Créer un rendez-vous dont le statut est déjà CANCELLED.
        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.now().plusDays(1),
                AppointmentStatus.CANCELLED
        );

        // Simuler la récupération du rendez-vous existant.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Act et Assert : une seconde annulation doit être refusée.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelAppointment(
                        1L,
                        UserRole.ADMIN,
                        null
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        assertEquals(
                "Only scheduled appointments can be cancelled.",
                exception.getReason()
        );

        // Vérifier qu'aucune sauvegarde supplémentaire n'a eu lieu.
        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldRejectAppointmentInThePast() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        AppointmentRequest request = createRequest(
                10L,
                LocalDate.now().minusDays(1),
                LocalTime.of(10, 0)
        );

        Patient patient = createPatient();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createAppointment(request, patient)
        );

        verifyNoInteractions(repository, doctorRepository);
    }


    @Test
    void shouldDeleteAppointment() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.of(2026, 10, 20),
                AppointmentStatus.SCHEDULED
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        service.deleteAppointment(1L);

        verify(repository).findById(1L);
        verify(repository).delete(appointment);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingAppointment() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        AppointmentNotFoundException exception = assertThrows(
                AppointmentNotFoundException.class,
                () -> service.deleteAppointment(999L)
        );

        assertEquals(
                "No appointment found with id: 999",
                exception.getMessage()
        );

        verify(repository, never()).delete(any(Appointment.class));
    }

    @Test
    void shouldRejectUpdatingAppointmentToPastDate() {
        // Arrange : créer les repositories simulés et le service.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        // Créer un rendez-vous initialement planifié dans le futur.
        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.now().plusDays(1),
                AppointmentStatus.SCHEDULED
        );

        // Simuler la recherche du rendez-vous existant.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Préparer une modification dont la date est passée.
        AppointmentRequest request = createRequest(
                20L,
                LocalDate.now().minusDays(1),
                LocalTime.of(11, 0)
        );

        // Act et Assert : la modification doit être refusée.
        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateAppointment(1L, request)
        );

        // Vérifier que le médecin n'a pas été recherché
        // et que le rendez-vous n'a pas été sauvegardé.
        verifyNoInteractions(doctorRepository);
        verify(repository, never()).save(any(Appointment.class));
    }

    @Test
    void shouldRejectUpdatingCompletedAppointment() {
        // Arrange : créer les repositories simulés et le service.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        // Créer un rendez-vous déjà terminé.
        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.now().plusDays(1),
                AppointmentStatus.COMPLETED
        );

        // Simuler la récupération du rendez-vous par son identifiant.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Préparer une demande de modification.
        AppointmentRequest request = createRequest(
                20L,
                LocalDate.now().plusDays(2),
                LocalTime.of(11, 0)
        );

        // Act et Assert : la modification doit être refusée.
        assertThrows(
                IllegalStateException.class,
                () -> service.updateAppointment(1L, request)
        );

        // Vérifier qu'aucun médecin n'a été recherché
        // et qu'aucune modification n'a été sauvegardée.
        verifyNoInteractions(doctorRepository);
        verify(repository, never()).save(any(Appointment.class));
    }

    @Test
    void shouldRejectUpdatingCancelledAppointment() {
        // Arrange : créer les repositories simulés et le service.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        // Créer un rendez-vous déjà annulé.
        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.now().plusDays(1),
                AppointmentStatus.CANCELLED
        );

        // Simuler la récupération du rendez-vous annulé.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Préparer une demande de modification.
        AppointmentRequest request = createRequest(
                20L,
                LocalDate.now().plusDays(2),
                LocalTime.of(11, 0)
        );

        // Act et Assert : la modification doit être refusée.
        assertThrows(
                IllegalStateException.class,
                () -> service.updateAppointment(1L, request)
        );

        // Vérifier qu'aucune opération de modification n'a eu lieu.
        verifyNoInteractions(doctorRepository);
        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldRejectUpdatingAppointmentWhenDoctorHasConflict() {
        // Arrange : créer les repositories simulés.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        // Simuler les repositories du planning et des exceptions.
        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);
        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        // Créer le service avec ses quatre dépendances.
        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        // Créer le patient propriétaire du rendez-vous à modifier.
        Patient patient = createPatient();

        // Créer le médecin initial et celui demandé pour la modification.
        Doctor originalDoctor = createDoctor("Dr", "Martin");
        Doctor updatedDoctor = createDoctor("Dr", "Franklin");

        // Choisir une date future pour la modification.
        LocalDate requestedDate = LocalDate.now().plusDays(7);

        // Créer le rendez-vous existant, qui est encore planifié.
        Appointment appointment = createAppointment(
                patient,
                originalDoctor,
                LocalDate.now().plusDays(1),
                AppointmentStatus.SCHEDULED
        );

        // Simuler la récupération du rendez-vous existant.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Simuler la recherche du nouveau médecin.
        when(doctorRepository.findById(20L))
                .thenReturn(Optional.of(updatedDoctor));

        // Préparer une demande de modification pour 10 h–10 h 30.
        AppointmentRequest request = createRequest(
                20L,
                requestedDate,
                LocalTime.of(10, 0)
        );

        // Définir les horaires habituels du nouveau médecin.
        DoctorSchedule schedule = new DoctorSchedule(
                updatedDoctor,
                requestedDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        // Simuler l'absence d'exception pour cette date.
        when(exceptionRepository.findByDoctor_IdAndDate(
                20L,
                requestedDate
        )).thenReturn(List.of());

        // Simuler un planning valide pour le nouveau créneau.
        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                20L,
                requestedDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        // Simuler un autre rendez-vous planifié qui chevauche
        // le créneau demandé pour le médecin.
        when(repository
                .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        20L,
                        requestedDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0),
                        appointment.getId()
                ))
                .thenReturn(true);

        // Act et Assert : la modification doit être refusée
        // puisque le nouveau créneau est déjà occupé.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateAppointment(1L, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        assertEquals(
                "The doctor already has an appointment during the requested time.",
                exception.getReason()
        );

        // Vérifier que le rendez-vous existant n'est pas sauvegardé.
        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldRejectUpdatingAppointmentWhenPatientHasConflict() {
        // Arrange : créer les repositories simulés.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        // Simuler les repositories des horaires et des exceptions.
        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);
        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        // Créer le service avec ses quatre dépendances.
        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        // Créer le patient qui possède le rendez-vous à modifier.
        Patient patient = createPatient();

        // Créer le médecin initial et le médecin demandé.
        Doctor originalDoctor = createDoctor("Dr", "Martin");
        Doctor updatedDoctor = createDoctor("Dr", "Franklin");

        // Choisir une date future pour la modification.
        LocalDate requestedDate = LocalDate.now().plusDays(7);

        // Créer le rendez-vous existant, encore planifié.
        Appointment appointment = createAppointment(
                patient,
                originalDoctor,
                LocalDate.now().plusDays(1),
                AppointmentStatus.SCHEDULED
        );

        // Simuler la récupération du rendez-vous à modifier.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Simuler la recherche du nouveau médecin.
        when(doctorRepository.findById(20L))
                .thenReturn(Optional.of(updatedDoctor));

        // Préparer une demande de déplacement à 10 h–10 h 30.
        AppointmentRequest request = createRequest(
                20L,
                requestedDate,
                LocalTime.of(10, 0)
        );

        // Définir le planning hebdomadaire du nouveau médecin.
        DoctorSchedule schedule = new DoctorSchedule(
                updatedDoctor,
                requestedDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        // Simuler l'absence d'exception pour cette date.
        when(exceptionRepository.findByDoctor_IdAndDate(
                20L,
                requestedDate
        )).thenReturn(List.of());

        // Simuler un créneau autorisé par le planning hebdomadaire.
        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                20L,
                requestedDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        // Simuler l'absence de conflit avec le nouveau médecin.
        when(repository
                .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        20L,
                        requestedDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0),
                        appointment.getId()
                ))
                .thenReturn(false);

        // Simuler un autre rendez-vous du patient qui chevauche
        // la période demandée, de 10 h à 10 h 30.
        when(repository
                .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        patient,
                        requestedDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0),
                        appointment.getId()
                ))
                .thenReturn(true);

        // Act et Assert : la modification doit être refusée,
        // car le patient a déjà un autre rendez-vous sur ce créneau.
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateAppointment(1L, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        assertEquals(
                "The patient already has an appointment during the requested time.",
                exception.getReason()
        );

        // Vérifier qu'aucune modification n'a été sauvegardée.
        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldUpdateAppointmentWhenNoConflictsExist() {
        // Arrange : créer les repositories simulés.
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        // Simuler les repositories du planning et des exceptions.
        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);
        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        // Créer le service avec ses quatre dépendances.
        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        // Créer le patient et les médecins concernés.
        Patient patient = createPatient();
        Doctor originalDoctor = createDoctor("Dr", "Martin");
        Doctor updatedDoctor = createDoctor("Dr", "Franklin");

        // Préparer une date future pour le nouveau rendez-vous.
        LocalDate requestedDate = LocalDate.now().plusDays(7);

        // Créer le rendez-vous initial, toujours planifié.
        Appointment appointment = createAppointment(
                patient,
                originalDoctor,
                LocalDate.now().plusDays(1),
                AppointmentStatus.SCHEDULED
        );

        // Simuler la récupération du rendez-vous à modifier.
        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        // Simuler la recherche du nouveau médecin.
        when(doctorRepository.findById(20L))
                .thenReturn(Optional.of(updatedDoctor));

        // Préparer la demande de modification : 10 h–10 h 30.
        AppointmentRequest request = createRequest(
                20L,
                requestedDate,
                LocalTime.of(10, 0)
        );

        // Définir le planning du nouveau médecin pour cette date.
        DoctorSchedule schedule = new DoctorSchedule(
                updatedDoctor,
                requestedDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        // Simuler l'absence d'exception à cette date.
        when(exceptionRepository.findByDoctor_IdAndDate(
                20L,
                requestedDate
        )).thenReturn(List.of());

        // Simuler un planning hebdomadaire valide.
        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                20L,
                requestedDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        // Simuler l'absence de conflit avec le médecin,
        // en excluant le rendez-vous en cours de modification.
        when(repository
                .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        20L,
                        requestedDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0),
                        appointment.getId()
                ))
                .thenReturn(false);

        // Simuler l'absence de conflit avec le patient,
        // en excluant également le rendez-vous en cours de modification.
        when(repository
                .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        patient,
                        requestedDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0),
                        appointment.getId()
                ))
                .thenReturn(false);

        // Simuler la sauvegarde du rendez-vous modifié.
        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act : demander la modification du rendez-vous.
        AppointmentResponse response =
                service.updateAppointment(1L, request);

        // Assert : vérifier les informations mises à jour.
        assertEquals("Dr Franklin", response.getDoctorName());
        assertEquals(requestedDate, response.getAppointmentDate());
        assertEquals(LocalTime.of(10, 0), response.getStartTime());
        assertEquals(LocalTime.of(10, 30), response.getEndTime());

        // Vérifier que les recherches de conflits ont été effectuées
        // avec l'identifiant du rendez-vous exclu.
        verify(repository)
                .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        20L,
                        requestedDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0),
                        appointment.getId()
                );

        verify(repository)
                .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        patient,
                        requestedDate,
                        AppointmentStatus.SCHEDULED,
                        LocalTime.of(10, 30),
                        LocalTime.of(10, 0),
                        appointment.getId()
                );

        // Vérifier que le rendez-vous modifié a été sauvegardé.
        verify(repository).save(appointment);
    }


    @Test
    void shouldUpdateAppointment() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        // Créer les mocks des repositories de planning et d'exceptions.
        DoctorScheduleRepository scheduleRepository =
                mock(DoctorScheduleRepository.class);

        DoctorScheduleExceptionRepository exceptionRepository =
                mock(DoctorScheduleExceptionRepository.class);

        // Créer le service avec les quatre repositories.
        AppointmentService service = createService(
                repository,
                doctorRepository,
                scheduleRepository,
                exceptionRepository
        );

        Patient patient = createPatient();
        Doctor originalDoctor = createDoctor("Dr", "Martin");
        Doctor updatedDoctor = createDoctor("Dr", "Franklin");

        Appointment appointment = createAppointment(
                patient,
                originalDoctor,
                LocalDate.of(2026, 10, 20),
                AppointmentStatus.SCHEDULED
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        when(doctorRepository.findById(20L))
                .thenReturn(Optional.of(updatedDoctor));

        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentRequest request = createRequest(
                20L,
                LocalDate.now().plusDays(7),
                LocalTime.of(11, 0)
        );

        // Configurer le planning du médecin demandé pour le jour choisi.
        // Le rendez-vous de 11 h à 11 h 30 doit être autorisé.
        LocalDate requestedDate = request.getAppointmentDate();

        DoctorSchedule schedule = new DoctorSchedule(
                updatedDoctor,
                requestedDate.getDayOfWeek(),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        // Simuler l'absence d'exception à cette date.
        when(exceptionRepository.findByDoctor_IdAndDate(
                20L,
                requestedDate
        )).thenReturn(List.of());

        // Simuler les horaires hebdomadaires du médecin.
        when(scheduleRepository.findByDoctor_IdAndDayOfWeek(
                20L,
                requestedDate.getDayOfWeek()
        )).thenReturn(List.of(schedule));

        request.setReason("Follow-up consultation");
        request.setNotes("Updated notes");

        AppointmentResponse response =
                service.updateAppointment(1L, request);

        assertEquals("Jean Dupont", response.getPatientName());
        assertEquals("Dr Franklin", response.getDoctorName());
        assertEquals(
                requestedDate,
                response.getAppointmentDate()
        );
        assertEquals(LocalTime.of(11, 0), response.getStartTime());
        assertEquals(LocalTime.of(11, 30), response.getEndTime());
        assertEquals("Follow-up consultation", response.getReason());
        assertEquals("Updated notes", response.getNotes());

        verify(repository).findById(1L);
        verify(doctorRepository).findById(20L);
        verify(repository).save(appointment);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingAppointment() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        AppointmentRequest request = createRequest(
                20L,
                LocalDate.of(2026, 10, 25),
                LocalTime.of(11, 0)
        );

        assertThrows(
                AppointmentNotFoundException.class,
                () -> service.updateAppointment(999L, request)
        );

        verify(repository, never()).save(any(Appointment.class));
        verifyNoInteractions(doctorRepository);
    }

    @Test
    void shouldMarkScheduledAppointmentAsCompleted() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.now().plusDays(1),
                AppointmentStatus.SCHEDULED
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        when(repository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response =
                service.updateAppointmentStatus(
                        1L,
                        AppointmentStatus.COMPLETED
                );

        assertEquals(
                AppointmentStatus.COMPLETED,
                response.getStatus()
        );

        verify(repository).save(appointment);
    }


    @Test
    void shouldRejectReopeningCompletedAppointment() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.now().plusDays(1),
                AppointmentStatus.COMPLETED
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(appointment));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateAppointmentStatus(
                        1L,
                        AppointmentStatus.SCHEDULED
                )
        );

        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldRejectReopeningCancelledAppointment() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();

        AppointmentService service =
                createService(repository, doctorRepository);

        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.now().plusDays(1),
                AppointmentStatus.CANCELLED
        );

        when(repository.findById(2L))
                .thenReturn(Optional.of(appointment));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateAppointmentStatus(
                        2L,
                        AppointmentStatus.SCHEDULED
                )
        );

        verify(repository, never()).save(any(Appointment.class));
    }


    @Test
    void shouldReturnAllAppointments() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.of(2026, 10, 20),
                AppointmentStatus.SCHEDULED
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Appointment> appointmentPage =
                new PageImpl<>(List.of(appointment), pageable, 1);

        when(repository.findAll(pageable)).thenReturn(appointmentPage);

        Page<AppointmentResponse> response =
                service.getAppointments(null, null, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(
                "Jean Dupont",
                response.getContent().get(0).getPatientName()
        );
        assertEquals(
                "Dr Martin",
                response.getContent().get(0).getDoctorName()
        );

        verify(repository).findAll(pageable);
    }

    @Test
    void shouldReturnAppointmentsByDoctorLastName() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.of(2026, 10, 20),
                AppointmentStatus.SCHEDULED
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Appointment> appointmentPage =
                new PageImpl<>(List.of(appointment), pageable, 1);

        when(repository.findByDoctor_LastName("Martin", pageable))
                .thenReturn(appointmentPage);

        Page<AppointmentResponse> response =
                service.getAppointments("Martin", null, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(
                "Dr Martin",
                response.getContent().get(0).getDoctorName()
        );

        verify(repository).findByDoctor_LastName("Martin", pageable);
    }

    @Test
    void shouldReturnAppointmentsByStatus() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.of(2026, 10, 20),
                AppointmentStatus.COMPLETED
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Appointment> appointmentPage =
                new PageImpl<>(List.of(appointment), pageable, 1);

        when(repository.findByStatus(
                AppointmentStatus.COMPLETED, pageable
        )).thenReturn(appointmentPage);

        Page<AppointmentResponse> response =
                service.getAppointments(
                        null, AppointmentStatus.COMPLETED, pageable
                );

        assertEquals(1, response.getTotalElements());
        assertEquals(
                AppointmentStatus.COMPLETED,
                response.getContent().get(0).getStatus()
        );

        verify(repository).findByStatus(
                AppointmentStatus.COMPLETED, pageable
        );
    }

    @Test
    void shouldReturnAppointmentsByDoctorAndStatus() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        Appointment appointment = createAppointment(
                createPatient(),
                createDoctor("Dr", "Martin"),
                LocalDate.of(2026, 10, 20),
                AppointmentStatus.COMPLETED
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Appointment> appointmentPage =
                new PageImpl<>(List.of(appointment), pageable, 1);

        when(repository.findByDoctor_LastNameAndStatus(
                "Martin", AppointmentStatus.COMPLETED, pageable
        )).thenReturn(appointmentPage);

        Page<AppointmentResponse> response =
                service.getAppointments(
                        "Martin", AppointmentStatus.COMPLETED, pageable
                );

        assertEquals(1, response.getTotalElements());
        assertEquals(
                "Dr Martin",
                response.getContent().get(0).getDoctorName()
        );
        assertEquals(
                AppointmentStatus.COMPLETED,
                response.getContent().get(0).getStatus()
        );

        verify(repository).findByDoctor_LastNameAndStatus(
                "Martin", AppointmentStatus.COMPLETED, pageable
        );
    }

    @Test
    void shouldReturnPaginatedAppointments() {
        AppointmentRepository repository = createAppointmentRepository();
        DoctorRepository doctorRepository = createDoctorRepository();
        AppointmentService service =
                createService(repository, doctorRepository);

        Patient patient = createPatient();
        Doctor doctor = createDoctor("Dr", "Martin");

        List<Appointment> appointments = java.util.stream.IntStream
                .range(0, 15)
                .mapToObj(i -> createAppointment(
                        patient,
                        doctor,
                        LocalDate.of(2026, 10, 1).plusDays(i),
                        AppointmentStatus.SCHEDULED
                ))
                .toList();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Appointment> appointmentPage = new PageImpl<>(
                appointments.subList(0, 10),
                pageable,
                15
        );

        when(repository.findAll(pageable))
                .thenReturn(appointmentPage);

        Page<AppointmentResponse> response =
                service.getAppointments(null, null, pageable);

        assertEquals(10, response.getNumberOfElements());
        assertEquals(15, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
        assertEquals(0, response.getPageable().getPageNumber());
        assertEquals(10, response.getPageable().getPageSize());

        verify(repository).findAll(pageable);
    }
}
