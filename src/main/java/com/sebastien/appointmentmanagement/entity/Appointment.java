package com.sebastien.appointmentmanagement.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String patientName;
    private String doctorName;
    private LocalDate appointmentDate;
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    protected Appointment(){
    }

    public Appointment(String patientName, String doctorName, LocalDate appointmentDate, AppointmentStatus status){
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.appointmentDate = appointmentDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }
    public String getPatientName() {
        return patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }


}
