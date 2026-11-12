package com.sebastien.appointmentmanagement.dto;

import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateAppointmentStatusRequest {

    @NotNull(message = "The new appointment status is required.")
    private AppointmentStatus status;

    @Size(
            max = 1000,
            message = "The change reason cannot exceed 1000 characters."
    )
    private String changeReason;

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}