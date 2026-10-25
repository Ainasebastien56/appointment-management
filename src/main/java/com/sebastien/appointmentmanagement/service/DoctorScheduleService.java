package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.dto.DoctorScheduleRequest;
import com.sebastien.appointmentmanagement.dto.DoctorScheduleResponse;
import com.sebastien.appointmentmanagement.entity.Doctor;
import com.sebastien.appointmentmanagement.entity.DoctorSchedule;
import com.sebastien.appointmentmanagement.entity.User;
import com.sebastien.appointmentmanagement.entity.UserRole;
import com.sebastien.appointmentmanagement.repository.DoctorRepository;
import com.sebastien.appointmentmanagement.repository.DoctorScheduleRepository;
import com.sebastien.appointmentmanagement.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;

@Service
public class DoctorScheduleService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public DoctorScheduleService(
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            DoctorScheduleRepository doctorScheduleRepository
    ) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
    }

    @Transactional
    public DoctorScheduleResponse createSchedule(
            Long doctorId,
            DoctorScheduleRequest request,
            String authenticatedEmail
    ) {
        // Vérifie le rôle réel de l'utilisateur connecté.
        User currentUser = userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found."
                ));

        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can manage doctor schedules."
            );
        }

        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();

        // L'heure de fin doit être postérieure à l'heure de début.
        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End time must be after start time."
            );
        }

        // Recherche le médecin concerné.
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Doctor not found."
                ));

        // Recherche les horaires déjà enregistrés pour ce jour.
        var existingSchedules =
                doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(
                        doctorId,
                        request.getDayOfWeek()
                );

        // Refuse les horaires qui chevauchent un horaire existant.
        boolean overlaps = existingSchedules.stream().anyMatch(existing ->
                startTime.isBefore(existing.getEndTime())
                        && endTime.isAfter(existing.getStartTime())
        );

        if (overlaps) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The schedule overlaps with an existing schedule."
            );
        }

        // Crée et enregistre le nouvel horaire.
        DoctorSchedule schedule = new DoctorSchedule(
                doctor,
                request.getDayOfWeek(),
                startTime,
                endTime
        );

        DoctorSchedule savedSchedule =
                doctorScheduleRepository.save(schedule);

        return new DoctorScheduleResponse(
                savedSchedule.getId(),
                doctor.getId(),
                savedSchedule.getDayOfWeek(),
                savedSchedule.getStartTime(),
                savedSchedule.getEndTime()
        );
    }
}