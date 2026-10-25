package com.sebastien.appointmentmanagement.repository;

import com.sebastien.appointmentmanagement.entity.DoctorScheduleException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DoctorScheduleExceptionRepository extends JpaRepository<DoctorScheduleException, Long> {

    List<DoctorScheduleException> findByDoctor_IdAndDate(
      Long doctorId,
      LocalDate date
    );
}
