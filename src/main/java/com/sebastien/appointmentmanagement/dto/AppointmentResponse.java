package com.sebastien.appointmentmanagement.dto;

import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import com.sebastien.appointmentmanagement.exception.AppointmentNotFoundException;

import java.time.LocalDate;

public class AppointmentResponse {
    private Long id;
    private String patientName;
    private String doctorName;
    private LocalDate appointmentDate;
    private AppointmentStatus status;

    public AppointmentResponse(){
    }

    public AppointmentResponse(Long id, String patientName, String doctorName, LocalDate appointmentDate, AppointmentStatus status) {
        this.id = id;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.appointmentDate = appointmentDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id){
        this.id = id;
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
