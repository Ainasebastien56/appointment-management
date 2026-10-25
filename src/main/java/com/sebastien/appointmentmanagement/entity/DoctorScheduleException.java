package com.sebastien.appointmentmanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class DoctorScheduleException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    private LocalDate date;

    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private ScheduleExceptionType type;

    private String reason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected DoctorScheduleException() {}

    public DoctorScheduleException(
            Doctor doctor,
            LocalDate date,
            ScheduleExceptionType type,
            LocalTime startTime,
            LocalTime endTime,
            String reason
    ) {
        this.doctor = doctor;
        this.date = date;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    // Getters

    public Long getId() {
        return id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public ScheduleExceptionType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
