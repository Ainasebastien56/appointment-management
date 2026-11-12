package com.sebastien.appointmentmanagement.dto;

public record DoctorAppointmentStatisticsResponse(
        long todayAppointments,
        long upcomingAppointments,
        long pendingOutcomes,
        long completedAppointments
) {
}
