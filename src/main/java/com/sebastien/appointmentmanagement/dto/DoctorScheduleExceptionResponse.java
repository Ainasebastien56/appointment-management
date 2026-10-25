package com.sebastien.appointmentmanagement.dto;

import com.sebastien.appointmentmanagement.entity.ScheduleExceptionType;

import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorScheduleExceptionResponse(
        Long id,
        Long doctorId,
        LocalDate date,
        ScheduleExceptionType type,
        LocalTime startTime,
        LocalTime endTime,
        String reason
) {
}