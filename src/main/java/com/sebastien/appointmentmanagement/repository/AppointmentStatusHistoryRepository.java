package com.sebastien.appointmentmanagement.repository;

import com.sebastien.appointmentmanagement.entity.AppointmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistory, Long> {

    List<AppointmentStatusHistory> findByAppointment_IdOrderByChangedAtDesc(
            Long appointmentId
    );
}
