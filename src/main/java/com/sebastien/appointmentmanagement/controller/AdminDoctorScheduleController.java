package com.sebastien.appointmentmanagement.controller;

import com.sebastien.appointmentmanagement.dto.DoctorScheduleRequest;
import com.sebastien.appointmentmanagement.dto.DoctorScheduleResponse;
import com.sebastien.appointmentmanagement.service.DoctorScheduleService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/doctors/{doctorId}/schedules")
public class AdminDoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    public AdminDoctorScheduleController(
            DoctorScheduleService doctorScheduleService
    ) {
        this.doctorScheduleService = doctorScheduleService;
    }

    @PostMapping
    public ResponseEntity<DoctorScheduleResponse> createSchedule(
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorScheduleRequest request,
            Authentication authentication
    ) {
        DoctorScheduleResponse response =
                doctorScheduleService.createSchedule(
                        doctorId,
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}