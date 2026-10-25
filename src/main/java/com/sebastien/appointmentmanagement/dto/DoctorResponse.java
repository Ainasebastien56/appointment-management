package com.sebastien.appointmentmanagement.dto;

import java.time.LocalDate;

public record DoctorResponse(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String specialty,
        String licenseNumber,
        String phone,
        LocalDate practiceStartDate
) {
}