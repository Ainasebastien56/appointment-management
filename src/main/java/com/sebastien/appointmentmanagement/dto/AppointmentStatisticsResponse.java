package com.sebastien.appointmentmanagement.dto;

public record AppointmentStatisticsResponse(
        long scheduledAppointments,
        long pendingOutcomes,
        long cancelledAppointments
) {
}
