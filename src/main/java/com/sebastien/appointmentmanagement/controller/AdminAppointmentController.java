
package com.sebastien.appointmentmanagement.controller;

import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.entity.Patient;
import com.sebastien.appointmentmanagement.entity.User;
import com.sebastien.appointmentmanagement.entity.UserRole;
import com.sebastien.appointmentmanagement.repository.PatientRepository;
import com.sebastien.appointmentmanagement.service.AppointmentService;
import com.sebastien.appointmentmanagement.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentController {

    private final CurrentUserService currentUserService;
    private final PatientRepository patientRepository;
    private final AppointmentService appointmentService;

    public AdminAppointmentController(
            CurrentUserService currentUserService,
            PatientRepository patientRepository,
            AppointmentService appointmentService) {
        this.currentUserService = currentUserService;
        this.patientRepository = patientRepository;
        this.appointmentService = appointmentService;
    }

    @PostMapping("/{patientId}")
    public AppointmentResponse createAppointmentForPatient(
            @PathVariable Long patientId,
            @Valid @RequestBody AppointmentRequest appointmentRequest,
            Authentication authentication) {

        User user = currentUserService.requireUser(authentication.getName());

        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can create appointments for patients."
            );
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient not found."
                ));

        return appointmentService.createAppointment(
                appointmentRequest,
                patient
        );
    }
}
