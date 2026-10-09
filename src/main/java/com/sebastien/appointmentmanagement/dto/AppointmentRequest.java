package com.sebastien.appointmentmanagement.dto;

import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


import java.time.LocalDate;

public class AppointmentRequest {
    @NotBlank
    private String patientName;

    @NotBlank
    private String doctorName;

    @NotNull
    private LocalDate appointmentDate;

    @NotNull
    private AppointmentStatus status;

    public AppointmentRequest(){
    }

    public AppointmentRequest(
            String patientName,
            String doctorName,
            LocalDate appointmentDate,
            AppointmentStatus status) {
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.appointmentDate = appointmentDate;
        this.status = status;
    }


    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
