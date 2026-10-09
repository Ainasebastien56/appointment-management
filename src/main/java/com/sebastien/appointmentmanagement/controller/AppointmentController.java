package com.sebastien.appointmentmanagement.controller;

import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.entity.AppointmentStatus;
import com.sebastien.appointmentmanagement.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService){
        this.appointmentService = appointmentService;
    }

    @GetMapping("/api/appointments")
    public Page<AppointmentResponse> getAppointments(
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false)AppointmentStatus status,
            Pageable pageable)
    {
        return appointmentService.getAppointments(doctorName, status, pageable);
    }

    @PostMapping("/api/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse createAppointment(@Valid @RequestBody AppointmentRequest appointmentRequest){
        return appointmentService.createAppointment(appointmentRequest);
    }

    @GetMapping("/api/appointments/{id}")
    public AppointmentResponse getAppointmentById(@PathVariable Long id){
        return appointmentService.getAppointmentById(id);
    }

    @DeleteMapping("/api/appointments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAppointment(@PathVariable Long id){
        appointmentService.deleteAppointment(id);
    }

    @PutMapping("/api/appointments/{id}")
    public  AppointmentResponse updateAppointment(@PathVariable Long id, @Valid @RequestBody AppointmentRequest appointmentRequest){
        return appointmentService.updateAppointment(id, appointmentRequest);
    }

}
