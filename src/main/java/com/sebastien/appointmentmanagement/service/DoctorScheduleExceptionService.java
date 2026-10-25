package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.dto.DoctorScheduleExceptionRequest;
import com.sebastien.appointmentmanagement.dto.DoctorScheduleExceptionResponse;
import com.sebastien.appointmentmanagement.entity.Doctor;
import com.sebastien.appointmentmanagement.entity.DoctorScheduleException;
import com.sebastien.appointmentmanagement.entity.ScheduleExceptionType;
import com.sebastien.appointmentmanagement.entity.User;
import com.sebastien.appointmentmanagement.entity.UserRole;
import com.sebastien.appointmentmanagement.repository.DoctorRepository;
import com.sebastien.appointmentmanagement.repository.DoctorScheduleExceptionRepository;
import com.sebastien.appointmentmanagement.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;

@Service
public class DoctorScheduleExceptionService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleExceptionRepository exceptionRepository;

    public DoctorScheduleExceptionService(
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            DoctorScheduleExceptionRepository exceptionRepository
    ) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.exceptionRepository = exceptionRepository;
    }

    @Transactional
    public DoctorScheduleExceptionResponse createException(
            Long doctorId,
            DoctorScheduleExceptionRequest request,
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
                    "Only administrators can manage schedule exceptions."
            );
        }

        // Vérifie que le médecin existe.
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Doctor not found."
                ));

        ScheduleExceptionType type = request.getType();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();

        // Valide les heures selon le type d'exception.
        if (type == ScheduleExceptionType.DAY_OFF) {
            if (startTime != null || endTime != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "DAY_OFF exceptions must not include startTime or endTime."
                );
            }
        } else if (type == ScheduleExceptionType.CUSTOM_HOURS) {
            if (startTime == null || endTime == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Start time and end time are required for CUSTOM_HOURS."
                );
            }

            if (!endTime.isAfter(startTime)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "End time must be after start time."
                );
            }
        }

        // Récupère les exceptions déjà enregistrées à cette date.
        List<DoctorScheduleException> existingExceptions =
                exceptionRepository.findByDoctor_IdAndDate(
                        doctorId,
                        request.getDate()
                );

        if (type == ScheduleExceptionType.DAY_OFF) {
            // Une journée entière indisponible ne peut pas coexister
            // avec d'autres exceptions horaires pour cette date.
            if (!existingExceptions.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "An exception already exists for this date."
                );
            }
        } else {
            // Une journée entière indisponible interdit des horaires particuliers.
            boolean hasDayOff = existingExceptions.stream()
                    .anyMatch(exception ->
                            exception.getType() == ScheduleExceptionType.DAY_OFF
                    );

            if (hasDayOff) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A DAY_OFF exception already exists for this date."
                );
            }

            // Refuse les périodes CUSTOM_HOURS qui se chevauchent.
            boolean overlaps = existingExceptions.stream()
                    .filter(exception ->
                            exception.getType() == ScheduleExceptionType.CUSTOM_HOURS
                    )
                    .anyMatch(exception ->
                            startTime.isBefore(exception.getEndTime())
                                    && endTime.isAfter(exception.getStartTime())
                    );

            if (overlaps) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The custom hours overlap with an existing exception."
                );
            }
        }

        // Crée et enregistre l'exception.
        DoctorScheduleException exception = new DoctorScheduleException(
                doctor,
                request.getDate(),
                type,
                startTime,
                endTime,
                request.getReason()
        );

        DoctorScheduleException saved =
                exceptionRepository.save(exception);

        return new DoctorScheduleExceptionResponse(
                saved.getId(),
                doctor.getId(),
                saved.getDate(),
                saved.getType(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getReason()
        );
    }
}