package com.sebastien.appointmentmanagement.repository;

import com.sebastien.appointmentmanagement.entity.Appointment;
import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import com.sebastien.appointmentmanagement.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    Page<Appointment> findByDoctor_LastName(
            String lastName,
            Pageable pageable
    );

    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByDoctor_LastNameAndStatus(
            String lastName,
            AppointmentStatus status,
            Pageable pageable
    );


    boolean existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long doctorId,
            LocalDate appointmentDate,
            AppointmentStatus status,
            LocalTime requestedEndTime,
            LocalTime requestedStartTime
    );

    boolean existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Patient patient,
            LocalDate appointmentDate,
            AppointmentStatus status,
            LocalTime requestedEndTime,
            LocalTime requestedStartTime
    );


    // Vérifier les conflits avec le même médecin,
    // en excluant le rendez-vous que nous sommes en train de modifier.
    boolean existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Long doctorId,
            LocalDate appointmentDate,
            AppointmentStatus status,
            LocalTime requestedEndTime,
            LocalTime requestedStartTime,
            Long excludedAppointmentId
    );

    // Vérifier les conflits avec le même patient,
    // en excluant également le rendez-vous actuel.
    boolean existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Patient patient,
            LocalDate appointmentDate,
            AppointmentStatus status,
            LocalTime requestedEndTime,
            LocalTime requestedStartTime,
            Long excludedAppointmentId
    );

}
