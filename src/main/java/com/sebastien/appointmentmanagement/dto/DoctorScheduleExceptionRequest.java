package com.sebastien.appointmentmanagement.dto;

import com.sebastien.appointmentmanagement.entity.ScheduleExceptionType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class DoctorScheduleExceptionRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Exception type is required")
    private ScheduleExceptionType type;

    private LocalTime startTime;

    private LocalTime endTime;

    private String reason;

    public DoctorScheduleExceptionRequest() {
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public ScheduleExceptionType getType() {
        return type;
    }

    public void setType(ScheduleExceptionType type) {
        this.type = type;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}