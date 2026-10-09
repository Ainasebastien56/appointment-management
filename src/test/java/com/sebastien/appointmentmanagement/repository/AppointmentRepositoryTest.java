package com.sebastien.appointmentmanagement.repository;


import com.sebastien.appointmentmanagement.entity.Appointment;
import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void contextLoads(){
    }

    @Test
    void shouldSaveAndFindAppointment() {
        // Arrange
        Appointment appointment = new Appointment(
                "John Doe",
                "Dr Martin",
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        // Act
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Assert
        assertNotNull(savedAppointment.getId());

        Optional<Appointment> foundAppointment =
                appointmentRepository.findById(savedAppointment.getId());

        assertTrue(foundAppointment.isPresent());
        assertEquals("John Doe", foundAppointment.get().getPatientName());
        assertEquals("Dr Martin", foundAppointment.get().getDoctorName());
        assertEquals(AppointmentStatus.SCHEDULED, foundAppointment.get().getStatus());
    }

    @Test
    void shouldFindAppointmentsByDoctorName() {
        // Arrange
        Appointment appointment1 = new Appointment(
                "John Doe",
                "Dr Martin",
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        Appointment appointment2 = new Appointment(
                "Jane Doe",
                "Dr Martin",
                LocalDate.of(2026, 10, 11),
                AppointmentStatus.COMPLETED
        );

        Appointment appointment3 = new Appointment(
                "Bob Smith",
                "Dr Dupont",
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        appointmentRepository.save(appointment1);
        appointmentRepository.save(appointment2);
        appointmentRepository.save(appointment3);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Appointment> results =
                appointmentRepository.findByDoctorName("Dr Martin", pageable);

        // Assert
        assertEquals(2, results.getTotalElements());
        assertEquals(2, results.getContent().size());

        assertTrue(
                results.getContent().stream()
                        .allMatch(appointment ->
                                appointment.getDoctorName().equals("Dr Martin"))
        );
    }

    @Test
    void shouldFindAppointmentsByStatus() {
        // Arrange
        Appointment appointment1 = new Appointment(
                "John Doe",
                "Dr Martin",
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        Appointment appointment2 = new Appointment(
                "Jane Doe",
                "Dr Martin",
                LocalDate.of(2026, 10, 11),
                AppointmentStatus.COMPLETED
        );

        Appointment appointment3 = new Appointment(
                "Bob Smith",
                "Dr Dupont",
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        appointmentRepository.save(appointment1);
        appointmentRepository.save(appointment2);
        appointmentRepository.save(appointment3);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Appointment> results =
                appointmentRepository.findByStatus(AppointmentStatus.SCHEDULED, pageable);

        // Assert
        assertEquals(2, results.getTotalElements());
        assertEquals(2, results.getContent().size());

        assertEquals(
                AppointmentStatus.SCHEDULED,
                results.getContent().get(0).getStatus()
        );
    }

    @Test
    void shouldFindAppointmentsByDoctorNameAndStatus() {
// Arrange
        Appointment appointment1 = new Appointment(
                "John Doe",
                "Dr Martin",
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        Appointment appointment2 = new Appointment(
                "Jane Doe",
                "Dr Martin",
                LocalDate.of(2026, 10, 11),
                AppointmentStatus.COMPLETED
        );

        Appointment appointment3 = new Appointment(
                "Bob Smith",
                "Dr Dupont",
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        Appointment appointment4 = new Appointment(
                "Alice Brown",
                "Dr Martin",
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        appointmentRepository.save(appointment1);
        appointmentRepository.save(appointment2);
        appointmentRepository.save(appointment3);
        appointmentRepository.save(appointment4);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Appointment> results =
                appointmentRepository.findByDoctorNameAndStatus("Dr Martin", AppointmentStatus.SCHEDULED, pageable);

        // Assert
        assertEquals(2, results.getTotalElements());
        assertEquals(2, results.getContent().size());

        assertEquals(
                "Dr Martin",
                results.getContent().get(0).getDoctorName()
        );

        assertEquals(
                AppointmentStatus.SCHEDULED,
                results.getContent().get(0).getStatus()
        );

        assertEquals(
                "Dr Martin",
                results.getContent().get(1).getDoctorName()
        );

        assertEquals(
                AppointmentStatus.SCHEDULED,
                results.getContent().get(1).getStatus()
        );

        List<String> patientNames = results.getContent().stream()
                .map(Appointment::getPatientName)
                .toList();

        assertTrue(patientNames.contains("John Doe"));
        assertTrue(patientNames.contains("Alice Brown"));
    }

    @Test
    void shouldPaginateAppointmentsByDoctorName() {

        Appointment appointment1 = new Appointment(
                "John Doe",
                "Dr Martin",
                LocalDate.of(2026, 10, 10),
                AppointmentStatus.SCHEDULED
        );

        Appointment appointment2 = new Appointment(
                "Jane Doe",
                "Dr Martin",
                LocalDate.of(2026, 10, 11),
                AppointmentStatus.COMPLETED
        );

        Appointment appointment3 = new Appointment(
                "Bob Smith",
                "Dr Martin",
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        Appointment appointment4 = new Appointment(
                "Alice Brown",
                "Dr Martin",
                LocalDate.of(2026, 10, 12),
                AppointmentStatus.SCHEDULED
        );

        Appointment appointment5 = new Appointment(
                "Tom Wilson",
                "Dr Martin",
                LocalDate.of(2026, 10, 13),
                AppointmentStatus.SCHEDULED
        );

        appointmentRepository.save(appointment1);
        appointmentRepository.save(appointment2);
        appointmentRepository.save(appointment3);
        appointmentRepository.save(appointment4);
        appointmentRepository.save(appointment5);

        Pageable pageable = PageRequest.of(0,2);

        Page<Appointment> results = appointmentRepository.findByDoctorName("Dr Martin", pageable);

        assertEquals(2, results.getContent().size());
        assertEquals(5, results.getTotalElements());
        assertEquals(3, results.getTotalPages());
    }
}
