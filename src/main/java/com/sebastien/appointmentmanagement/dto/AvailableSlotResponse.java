package com.sebastien.appointmentmanagement.dto;

import java.time.LocalTime;

public record AvailableSlotResponse (
    Long doctorId,
    String doctorName,
    LocalTime startTime,
    LocalTime endTime
) {}
