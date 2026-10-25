
package com.sebastien.appointmentmanagement.repository;

import com.sebastien.appointmentmanagement.entity.Appointment;
import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import com.sebastien.appointmentmanagement.entity.Doctor;
import com.sebastien.appointmentmanagement.entity.Gender;
import com.sebastien.appointmentmanagement.entity.Patient;
import com.sebastien.appointmentmanagement.entity.User;
import com.sebastien.appointmentmanagement.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldSaveAndFindAppointment() {
        // Arrange
        Doctor doctor = createDoctor("Martin");

        Appointment appointment = saveAppointment(
                "John",
                "Doe",
                doctor,
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        // Act
        Long appointmentId = appointment.getId();
        entityManager.clear();

        Optional<Appointment> foundAppointment =
                appointmentRepository.findById(appointmentId);

        // Assert
        assertTrue(foundAppointment.isPresent());

        Appointment found = foundAppointment.orElseThrow();

        assertEquals("John", found.getPatient().getFirstName());
        assertEquals("Doe", found.getPatient().getLastName());
        assertEquals("Dr", found.getDoctor().getFirstName());
        assertEquals("Martin", found.getDoctor().getLastName());
        assertEquals(
                AppointmentStatus.SCHEDULED,
                found.getStatus()
        );
    }

    @Test
    void shouldFindAppointmentsByDoctorLastName() {
        // Arrange
        Doctor doctorMartin = createDoctor("Martin");
        Doctor doctorDupont = createDoctor("Dupont");

        saveAppointment(
                "John", "Doe", doctorMartin,
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        saveAppointment(
                "Jane", "Doe", doctorMartin,
                LocalDate.of(2026, 10, 11),
                AppointmentStatus.COMPLETED
        );

        saveAppointment(
                "Bob", "Smith", doctorDupont,
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Appointment> results =
                appointmentRepository.findByDoctor_LastName(
                        "Martin", pageable
                );

        // Assert
        assertEquals(2, results.getTotalElements());
        assertEquals(2, results.getContent().size());

        assertTrue(
                results.getContent().stream()
                        .allMatch(appointment ->
                                appointment.getDoctor()
                                        .getLastName()
                                        .equals("Martin"))
        );
    }

    @Test
    void shouldFindAppointmentsByStatus() {
        // Arrange
        Doctor doctorMartin = createDoctor("Martin");
        Doctor doctorDupont = createDoctor("Dupont");

        saveAppointment(
                "John", "Doe", doctorMartin,
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        saveAppointment(
                "Jane", "Doe", doctorMartin,
                LocalDate.of(2026, 10, 11),
                AppointmentStatus.COMPLETED
        );

        saveAppointment(
                "Bob", "Smith", doctorDupont,
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Appointment> results =
                appointmentRepository.findByStatus(
                        AppointmentStatus.SCHEDULED, pageable
                );

        // Assert
        assertEquals(2, results.getTotalElements());
        assertEquals(2, results.getContent().size());

        assertTrue(
                results.getContent().stream()
                        .allMatch(appointment ->
                                appointment.getStatus()
                                        == AppointmentStatus.SCHEDULED)
        );
    }

    @Test
    void shouldFindAppointmentsByDoctorLastNameAndStatus() {
        // Arrange
        Doctor doctorMartin = createDoctor("Martin");
        Doctor doctorDupont = createDoctor("Dupont");

        saveAppointment(
                "John", "Doe", doctorMartin,
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        saveAppointment(
                "Jane", "Doe", doctorMartin,
                LocalDate.of(2026, 10, 11),
                AppointmentStatus.COMPLETED
        );

        saveAppointment(
                "Bob", "Smith", doctorDupont,
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        saveAppointment(
                "Alice", "Brown", doctorMartin,
                LocalDate.of(2026, 10, 13),
                AppointmentStatus.SCHEDULED
        );

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Appointment> results =
                appointmentRepository
                        .findByDoctor_LastNameAndStatus(
                                "Martin",
                                AppointmentStatus.SCHEDULED,
                                pageable
                        );

        // Assert
        assertEquals(2, results.getTotalElements());
        assertEquals(2, results.getContent().size());

        assertTrue(
                results.getContent().stream()
                        .allMatch(appointment ->
                                appointment.getDoctor()
                                        .getLastName()
                                        .equals("Martin")
                                        && appointment.getStatus()
                                        == AppointmentStatus.SCHEDULED)
        );

        List<String> patientNames = results.getContent().stream()
                .map(appointment ->
                        appointment.getPatient().getFirstName()
                                + " "
                                + appointment.getPatient().getLastName())
                .toList();

        assertTrue(patientNames.contains("John Doe"));
        assertTrue(patientNames.contains("Alice Brown"));
    }

    @Test
    void shouldPaginateAppointmentsByDoctorLastName() {
        // Arrange
        Doctor doctorMartin = createDoctor("Martin");

        saveAppointment(
                "John", "Doe", doctorMartin,
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        saveAppointment(
                "Jane", "Doe", doctorMartin,
                LocalDate.of(2026, 10, 11),
                AppointmentStatus.COMPLETED
        );

        saveAppointment(
                "Bob", "Smith", doctorMartin,
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        saveAppointment(
                "Alice", "Brown", doctorMartin,
                LocalDate.of(2026, 10, 13),
                AppointmentStatus.SCHEDULED
        );

        saveAppointment(
                "Tom", "Wilson", doctorMartin,
                LocalDate.of(2026, 10, 14),
                AppointmentStatus.SCHEDULED
        );

        Pageable pageable = PageRequest.of(0, 2);

        // Act
        Page<Appointment> results =
                appointmentRepository.findByDoctor_LastName(
                        "Martin", pageable
                );

        // Assert
        assertEquals(2, results.getContent().size());
        assertEquals(5, results.getTotalElements());
        assertEquals(3, results.getTotalPages());
    }

    @Test
    void shouldDetectOverlappingScheduledAppointmentForDoctorAndPatient() {
        Doctor doctor = createDoctor("Martin");
        LocalDate date = LocalDate.now().plusDays(7);

        Appointment existing = saveAppointment(
                "John",
                "Doe",
                doctor,
                date,
                AppointmentStatus.SCHEDULED
        );

        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(10, 30));
        appointmentRepository.saveAndFlush(existing);

        boolean doctorConflict =
                appointmentRepository
                        .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                doctor.getId(),
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(10, 45),
                                LocalTime.of(10, 15)
                        );

        boolean patientConflict =
                appointmentRepository
                        .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                existing.getPatient(),
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(10, 45),
                                LocalTime.of(10, 15)
                        );

        assertTrue(doctorConflict);
        assertTrue(patientConflict);
    }


    @Test
    void shouldIgnoreCancelledAppointmentWhenCheckingConflicts() {
        Doctor doctor = createDoctor("Martin");
        LocalDate date = LocalDate.now().plusDays(7);

        Appointment cancelled = saveAppointment(
                "John",
                "Doe",
                doctor,
                date,
                AppointmentStatus.CANCELLED
        );

        cancelled.setStartTime(LocalTime.of(10, 0));
        cancelled.setEndTime(LocalTime.of(10, 30));
        appointmentRepository.saveAndFlush(cancelled);

        boolean doctorConflict =
                appointmentRepository
                        .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                doctor.getId(),
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(10, 45),
                                LocalTime.of(10, 15)
                        );

        boolean patientConflict =
                appointmentRepository
                        .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                cancelled.getPatient(),
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(10, 45),
                                LocalTime.of(10, 15)
                        );

        assertFalse(doctorConflict);
        assertFalse(patientConflict);
    }

    @Test
    void shouldNotDetectConflictForAdjacentAppointments() {
        Doctor doctor = createDoctor("Martin");
        LocalDate date = LocalDate.now().plusDays(7);

        Appointment existing = saveAppointment(
                "John",
                "Doe",
                doctor,
                date,
                AppointmentStatus.SCHEDULED
        );

        existing.setStartTime(LocalTime.of(10, 0));
        existing.setEndTime(LocalTime.of(10, 30));
        appointmentRepository.saveAndFlush(existing);

        boolean doctorConflict =
                appointmentRepository
                        .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                doctor.getId(),
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(11, 0),
                                LocalTime.of(10, 30)
                        );

        boolean patientConflict =
                appointmentRepository
                        .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                existing.getPatient(),
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(11, 0),
                                LocalTime.of(10, 30)
                        );

        assertFalse(doctorConflict);
        assertFalse(patientConflict);
    }


    @Test
    void shouldKeepCancelledAppointmentInDatabase() {
        // Arrange : créer un médecin et un rendez-vous planifié.
        Doctor doctor = createDoctor("Martin");

        Appointment appointment = saveAppointment(
                "John",
                "Doe",
                doctor,
                LocalDate.now().plusDays(7),
                AppointmentStatus.SCHEDULED
        );

        // Conserver l'identifiant pour retrouver le rendez-vous ensuite.
        Long appointmentId = appointment.getId();

        // Act : annuler le rendez-vous en changeant uniquement son statut.
        appointment.setStatus(AppointmentStatus.CANCELLED);

        // Enregistrer le changement dans la base de données de test.
        appointmentRepository.saveAndFlush(appointment);

        // Vider le contexte JPA pour forcer une nouvelle lecture en base.
        entityManager.clear();

        // Rechercher le rendez-vous avec son identifiant d'origine.
        Optional<Appointment> foundAppointment =
                appointmentRepository.findById(appointmentId);

        // Assert : vérifier que le rendez-vous existe toujours.
        assertTrue(foundAppointment.isPresent());

        // Vérifier que son statut est bien CANCELLED.
        assertEquals(
                AppointmentStatus.CANCELLED,
                foundAppointment.orElseThrow().getStatus()
        );
    }


    @Test
    void shouldExcludeCurrentAppointmentButDetectOtherConflicts() {
        // Arrange : créer deux médecins et le patient propriétaire
        // du rendez-vous que nous allons modifier.
        Doctor doctor = createDoctor("Martin");
        Doctor otherDoctor = createDoctor("Dupont");
        Patient patient = createPatient("John", "Doe");

        // Utiliser une date future pour isoler le test des dates passées.
        LocalDate date = LocalDate.now().plusDays(7);

        // Créer le rendez-vous existant sur un créneau valide.
        Appointment currentAppointment = new Appointment(
                patient,
                doctor,
                date,
                LocalTime.of(10, 0),  // Ancien début : 10 h 00.
                LocalTime.of(10, 30), // Ancienne fin : 10 h 30.
                "Routine check-up",
                AppointmentStatus.SCHEDULED
        );

        appointmentRepository.saveAndFlush(currentAppointment);

        // Vérifier les conflits pour le nouveau créneau : 10 h 30–11 h.
        // On exclut l'identifiant du rendez-vous en cours de modification.
        boolean doctorConflictBefore =
                appointmentRepository
                        .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                                doctor.getId(),
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(11, 0),
                                LocalTime.of(10, 30),
                                currentAppointment.getId()
                        );

        boolean patientConflictBefore =
                appointmentRepository
                        .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                                patient,
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(11, 0),
                                LocalTime.of(10, 30),
                                currentAppointment.getId()
                        );

        // Le seul rendez-vous existant est exclu : aucun autre conflit
        // ne doit être trouvé à ce stade.
        assertFalse(doctorConflictBefore);
        assertFalse(patientConflictBefore);

        // Créer un autre rendez-vous qui bloque le nouveau créneau.
        Appointment otherDoctorAppointment = new Appointment(
                createPatient("Jane", "Brown"),
                doctor,
                date,
                LocalTime.of(10, 30), // Début du créneau demandé.
                LocalTime.of(11, 0),  // Fin du créneau demandé.
                "Routine check-up",
                AppointmentStatus.SCHEDULED
        );

        appointmentRepository.saveAndFlush(otherDoctorAppointment);

        // Créer un autre rendez-vous pour le même patient,
        // cette fois avec un médecin différent, sur le même intervalle.
        Appointment otherPatientAppointment = new Appointment(
                patient,
                otherDoctor,
                date,
                LocalTime.of(10, 30), // Début du créneau demandé.
                LocalTime.of(11, 0),  // Fin du créneau demandé.
                "Follow-up consultation",
                AppointmentStatus.SCHEDULED
        );

        appointmentRepository.saveAndFlush(otherPatientAppointment);

        // Rechercher de nouveau les conflits, en excluant toujours
        // uniquement le rendez-vous que nous voulons modifier.
        boolean doctorConflictAfter =
                appointmentRepository
                        .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                                doctor.getId(),
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(11, 0),
                                LocalTime.of(10, 30),
                                currentAppointment.getId()
                        );

        boolean patientConflictAfter =
                appointmentRepository
                        .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                                patient,
                                date,
                                AppointmentStatus.SCHEDULED,
                                LocalTime.of(11, 0),
                                LocalTime.of(10, 30),
                                currentAppointment.getId()
                        );

        // Assert : les autres rendez-vous qui chevauchent le créneau
        // doivent être détectés pour le médecin et pour le patient.
        assertTrue(doctorConflictAfter);
        assertTrue(patientConflictAfter);
    }


    private Doctor createDoctor(String lastName) {
        User user = new User(
                "doctor." + lastName.toLowerCase() + "@example.com",
                "test-password",
                UserRole.DOCTOR
        );

        entityManager.persist(user);

        Doctor doctor = new Doctor(
                user,
                "Dr",
                lastName,
                "General Practice",
                "LIC-" + lastName.toUpperCase(),
                "555-0100",
                LocalDate.of(2020, 1, 1)
        );

        entityManager.persist(doctor);
        entityManager.flush();

        return doctor;
    }

    private Patient createPatient(
            String firstName,
            String lastName) {

        User user = new User(
                "patient."
                        + firstName.toLowerCase()
                        + "."
                        + lastName.toLowerCase()
                        + "@example.com",
                "test-password",
                UserRole.PATIENT
        );

        entityManager.persist(user);

        Patient patient = new Patient(
                user,
                firstName,
                lastName,
                LocalDate.of(1990, 1, 1),
                Gender.MALE,
                "555-0200"
        );

        entityManager.persist(patient);
        entityManager.flush();

        return patient;
    }

    private Appointment saveAppointment(
            String patientFirstName,
            String patientLastName,
            Doctor doctor,
            LocalDate appointmentDate,
            AppointmentStatus status) {

        Patient patient = createPatient(
                patientFirstName,
                patientLastName
        );

        Appointment appointment = new Appointment(
                patient,
                doctor,
                appointmentDate,
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                "Routine check-up",
                status
        );

        return appointmentRepository.saveAndFlush(appointment);
    }
}
