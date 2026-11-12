package com.sebastien.appointmentmanagement.dto;

import jakarta.validation.constraints.Size;

// Corps facultatif de DELETE /api/appointments/{id}.
public class CancelAppointmentRequest {

    @Size(
            max = 1000,
            message = "The cancellation reason cannot exceed 1000 characters."
    )
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
