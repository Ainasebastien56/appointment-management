package com.sebastien.appointmentmanagement.repository;

import com.sebastien.appointmentmanagement.entity.Appointment;
import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> findByDoctorName(String doctorName, Pageable pageable);

    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByDoctorNameAndStatus(String doctorName, AppointmentStatus status, Pageable pageable);
}
