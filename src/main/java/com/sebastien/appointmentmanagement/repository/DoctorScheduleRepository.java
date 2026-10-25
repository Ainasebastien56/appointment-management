package com.sebastien.appointmentmanagement.repository;

import com.sebastien.appointmentmanagement.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctor_IdAndDayOfWeek(
            Long doctorId,
            DayOfWeek dayOfWeek
    );
}
