package com.sebastien.appointmentmanagement.controller;

import com.sebastien.appointmentmanagement.dto.DoctorScheduleExceptionRequest;
import com.sebastien.appointmentmanagement.dto.DoctorScheduleExceptionResponse;
import com.sebastien.appointmentmanagement.service.DoctorScheduleExceptionService;

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
@RequestMapping(
        "/api/admin/doctors/{doctorId}/schedule-exceptions"
)
public class AdminDoctorScheduleExceptionController {

    private final DoctorScheduleExceptionService exceptionService;

    public AdminDoctorScheduleExceptionController(
            DoctorScheduleExceptionService exceptionService
    ) {
        this.exceptionService = exceptionService;
    }

    @PostMapping
    public ResponseEntity<DoctorScheduleExceptionResponse> createException(
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorScheduleExceptionRequest request,
            Authentication authentication
    ) {
        DoctorScheduleExceptionResponse response =
                exceptionService.createException(
                        doctorId,
                        request,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}