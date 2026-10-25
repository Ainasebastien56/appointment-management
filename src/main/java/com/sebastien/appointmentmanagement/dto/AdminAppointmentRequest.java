package com.sebastien.appointmentmanagement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AdminAppointmentRequest {

    // Identifiant du patient choisi par l'administrateur.
    @NotNull
    @Positive
    private Long patientId;

    // Données de réservation déjà définies dans le DTO existant.
    @NotNull
    @Valid
    private AppointmentRequest appointment;

    public AdminAppointmentRequest() {
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public AppointmentRequest getAppointment() {
        return appointment;
    }

    public void setAppointment(AppointmentRequest appointment) {
        this.appointment = appointment;
    }
}
