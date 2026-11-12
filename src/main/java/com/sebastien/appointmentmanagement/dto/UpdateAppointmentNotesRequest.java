package com.sebastien.appointmentmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateAppointmentNotesRequest {

    @NotBlank(message = "Appointment notes cannot be blank.")
    @Size(
            max = 2000,
            message = "Appointment notes cannot exceed 2000 characters."
    )
    private String notes;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
