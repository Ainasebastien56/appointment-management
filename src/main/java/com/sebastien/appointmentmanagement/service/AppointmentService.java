
package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.entity.*;
import com.sebastien.appointmentmanagement.exception.AppointmentNotFoundException;
import com.sebastien.appointmentmanagement.repository.AppointmentRepository;
import com.sebastien.appointmentmanagement.repository.DoctorRepository;
import com.sebastien.appointmentmanagement.repository.DoctorScheduleRepository;
import com.sebastien.appointmentmanagement.repository.DoctorScheduleExceptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Importe les outils de construction des critères SQL.
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorScheduleExceptionRepository doctorScheduleExceptionRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            DoctorScheduleRepository doctorScheduleRepository,
            DoctorScheduleExceptionRepository doctorScheduleExceptionRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.doctorScheduleExceptionRepository = doctorScheduleExceptionRepository;
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();

        response.setId(appointment.getId());

        Patient patient = appointment.getPatient();
        response.setPatientId(patient.getId());
        response.setPatientName(
                fullName(patient.getFirstName(), patient.getLastName())
        );

        Doctor doctor = appointment.getDoctor();
        response.setDoctorId(doctor.getId());
        response.setDoctorName(
                fullName(doctor.getFirstName(), doctor.getLastName())
        );

        response.setAppointmentDate(appointment.getAppointmentDate());
        response.setStartTime(appointment.getStartTime());
        response.setEndTime(appointment.getEndTime());
        response.setType(appointment.getType());
        response.setReason(appointment.getReason());
        response.setStatus(appointment.getStatus());
        response.setNotes(appointment.getNotes());
        response.setCreatedAt(appointment.getCreatedAt());
        response.setUpdatedAt(appointment.getUpdatedAt());

        return response;
    }

    private String fullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();

        return (first + " " + last).trim();
    }

    private void validateAppointmentNotInPast(
            LocalDate appointmentDate,
            LocalTime startTime
    ){
        LocalDateTime appointmentStart = LocalDateTime.of(appointmentDate, startTime);

        if(appointmentStart.isBefore(LocalDateTime.now())){
            throw new IllegalArgumentException(
              "Appointment date and time cannot be in the past."
            );
        }
    }


    private void validateAppointmentWithinSchedule(
            Long doctorId,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime
    ) {

        List<DoctorScheduleException> exceptions = doctorScheduleExceptionRepository.findByDoctor_IdAndDate(
                doctorId,
                appointmentDate
        );

        boolean dayOff = exceptions.stream().anyMatch(exception ->
                exception.getType() == ScheduleExceptionType.DAY_OFF);

        if (dayOff) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The doctor is not available on the requested date."
            );
        }

        List<DoctorScheduleException> customHours = exceptions.stream()
                .filter(exception ->
                        exception.getType() == ScheduleExceptionType.CUSTOM_HOURS
                ).toList();

        if (!customHours.isEmpty()){

            boolean fitsCustomHours = customHours.stream().anyMatch(exception ->
                    exception.getStartTime() != null
                            && exception.getEndTime() != null
                            && fitsSlotInterval(
                            exception.getStartTime(),
                            exception.getEndTime(),
                            startTime,
                            endTime
                    )
            );
            if (!fitsCustomHours) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The requested time is outside the doctor's exceptional working hours."
                );
            }

            return;
        }

        List<DoctorSchedule> schedules =
                doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(
                        doctorId,
                        appointmentDate.getDayOfWeek()
                );


        boolean fitsSchedule = schedules.stream().anyMatch(schedule ->
                fitsSlotInterval(
                        schedule.getStartTime(),
                        schedule.getEndTime(),
                        startTime,
                        endTime
                )
        );
        if (!fitsSchedule) {
            throw new IllegalArgumentException(
                    "The requested time is outside the doctor's working hours."
            );
        }
    }


    private void validateNoAppointmentConflict(
            Long doctorId,
            Patient patient,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        boolean doctorHasConflict =
                appointmentRepository
                        .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                doctorId,
                                appointmentDate,
                                AppointmentStatus.SCHEDULED,
                                endTime,
                                startTime
                        );

        if (doctorHasConflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The doctor already has an appointment during the requested time."
            );
        }

        boolean patientHasConflict =
                appointmentRepository
                        .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                patient,
                                appointmentDate,
                                AppointmentStatus.SCHEDULED,
                                endTime,
                                startTime
                        );

        if (patientHasConflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The patient already has an appointment during the requested time."
            );
        }
    }


    /**
     * Vérifie les conflits lors de la modification d'un rendez-vous.
     * L'identifiant du rendez-vous modifié est exclu des recherches
     * pour éviter qu'il soit considéré comme un conflit avec lui-même.
     */
    private void validateNoAppointmentConflictForUpdate(
            Long appointmentId,
            Long doctorId,
            Patient patient,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        // Rechercher un autre rendez-vous planifié qui chevauche
        // le créneau demandé pour le médecin sélectionné.
        boolean doctorHasConflict =
                appointmentRepository
                        .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                                doctorId,
                                appointmentDate,
                                AppointmentStatus.SCHEDULED,
                                endTime,
                                startTime,
                                appointmentId
                        );

        // Refuser la modification si le médecin est déjà occupé.
        if (doctorHasConflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The doctor already has an appointment during the requested time."
            );
        }

        // Rechercher un autre rendez-vous planifié qui chevauche
        // le créneau demandé pour le même patient.
        boolean patientHasConflict =
                appointmentRepository
                        .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                                patient,
                                appointmentDate,
                                AppointmentStatus.SCHEDULED,
                                endTime,
                                startTime,
                                appointmentId
                        );

        // Refuser la modification si le patient a un autre rendez-vous
        // planifié pendant cette période.
        if (patientHasConflict) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The patient already has an appointment during the requested time."
            );
        }
    }


    /**
     * Vérifie qu'un rendez-vous tient dans un intervalle de planning
     * et commence sur une frontière de créneau de 30 minutes.
     */
    private boolean fitsSlotInterval(
            LocalTime intervalStart,
            LocalTime intervalEnd,
            LocalTime requestedStart,
            LocalTime requestedEnd
    ) {
        // Refuser si le rendez-vous commence avant le créneau
        // ou se termine après sa limite.
        if (requestedStart.isBefore(intervalStart)
                || requestedEnd.isAfter(intervalEnd)) {
            return false;
        }

        // Calculer le nombre de minutes entre le début de l'intervalle
        // et le début du rendez-vous demandé.
        long minutesFromIntervalStart =
                Duration.between(intervalStart, requestedStart).toMinutes();

        // Calculer la durée réelle du rendez-vous demandé.
        long appointmentDuration =
                Duration.between(requestedStart, requestedEnd).toMinutes();

        // Le rendez-vous doit durer exactement 30 minutes
        // et commencer sur un créneau de 30 minutes.
        return appointmentDuration == 30
                && minutesFromIntervalStart % 30 == 0;
    }

    // Retourne les rendez-vous d'un patient et applique les filtres demandés.
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForPatient(
            Long patientId,
            String doctorLastName,
            AppointmentStatus status,
            Pageable pageable) {

        // Limite obligatoirement les résultats au patient identifié par le serveur.
        return getAppointmentsScoped(
                patientId, null, doctorLastName, status, pageable
        );
    }

    // Retourne les rendez-vous associés au profil du médecin connecté.
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForDoctor(
            Long doctorId,
            AppointmentStatus status,
            Pageable pageable) {

        // Limite obligatoirement les résultats au médecin identifié par le serveur.
        return getAppointmentsScoped(
                null, doctorId, null, status, pageable
        );
    }

    // Construit les critères de recherche et convertit les entités en DTO.
    private Page<AppointmentResponse> getAppointmentsScoped(
            Long patientId,
            Long doctorId,
            String doctorLastName,
            AppointmentStatus status,
            Pageable pageable) {

        // Prépare une recherche dynamique pour la table appointment.
        Specification<Appointment> specification = (root, query, criteriaBuilder) -> {

            // Stocke les conditions SQL à appliquer.
            List<Predicate> predicates = new ArrayList<>();

            // Si un patient est fourni, limite les résultats à son identifiant.
            if (patientId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("patient").get("id"),
                        patientId
                ));
            }

            // Si un médecin est fourni, limite les résultats à son identifiant.
            if (doctorId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("doctor").get("id"),
                        doctorId
                ));
            }

            // Applique éventuellement le filtre de statut.
            if (status != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"),
                        status
                ));
            }

            // Applique éventuellement le filtre sur le nom du médecin.
            if (doctorLastName != null && !doctorLastName.isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("doctor").get("lastName")),
                        doctorLastName.trim().toLowerCase(Locale.ROOT)
                ));
            }

            // Combine toutes les conditions avec AND.
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        // Exécute la recherche paginée puis convertit les résultats en DTO.
        return appointmentRepository.findAll(specification, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointments(
            String doctorLastName,
            AppointmentStatus status,
            Pageable pageable) {

        Page<Appointment> appointments;

        if (doctorLastName == null && status == null) {
            appointments = appointmentRepository.findAll(pageable);
        } else if (doctorLastName != null && status == null) {
            appointments = appointmentRepository.findByDoctor_LastName(
                    doctorLastName, pageable
            );
        } else if (doctorLastName == null) {
            appointments = appointmentRepository.findByStatus(
                    status, pageable
            );
        } else {
            appointments =
                    appointmentRepository.findByDoctor_LastNameAndStatus(
                            doctorLastName, status, pageable
                    );
        }

        return appointments.map(this::toResponse);
    }

    /*
     * Temporary: appointment creation cannot be connected to the API
     * until the authenticated patient's profile is available.
     */
    public AppointmentResponse createAppointment(
            AppointmentRequest request) {
        throw new IllegalStateException(
                "Appointment creation requires an authenticated patient."
        );
    }

    /*
     * Creates an appointment for a patient already resolved by the backend.
     * The authentication layer will supply this patient later.
     */
    public AppointmentResponse createAppointment(
            AppointmentRequest request,
            Patient patient) {

        if (patient == null) {
            throw new IllegalArgumentException(
                    "Patient is required to create an appointment."
            );
        }

        validateAppointmentNotInPast(
                request.getAppointmentDate(),
                request.getStartTime()
        );

        Doctor doctor = findDoctorById(request.getDoctorId());

        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(30);

        validateAppointmentWithinSchedule(
                request.getDoctorId(),
                request.getAppointmentDate(),
                startTime,
                endTime
        );

        validateNoAppointmentConflict(
                request.getDoctorId(),
                patient,
                request.getAppointmentDate(),
                startTime,
                endTime
        );

        Appointment appointment = new Appointment(
                patient,
                doctor,
                request.getAppointmentDate(),
                startTime,
                endTime,
                request.getReason(),
                AppointmentStatus.SCHEDULED
        );

        appointment.setType(request.getType());
        appointment.setNotes(request.getNotes());

        Appointment savedAppointment =
                appointmentRepository.save(appointment);

        return toResponse(savedAppointment);
    }

    private Doctor findDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Doctor not found with ID: " + doctorId
                        )
                );
    }

    private Appointment findAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    // Vérifie que le profil demandé est autorisé à consulter ce rendez-vous.
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentByIdForUser(
            Long id,
            UserRole role,
            Long profileId) {

        // Retrouve le rendez-vous ou lève l'exception 404 existante.
        Appointment appointment = findAppointmentById(id);

        // Un administrateur peut consulter tous les rendez-vous.
        boolean allowed = role == UserRole.ADMIN;

        // Un patient ne peut consulter que son propre rendez-vous.
        if (role == UserRole.PATIENT && profileId != null) {
            allowed = profileId.equals(appointment.getPatient().getId());
        }

        // Un médecin ne peut consulter que les rendez-vous de son profil.
        if (role == UserRole.DOCTOR && profileId != null) {
            allowed = profileId.equals(appointment.getDoctor().getId());
        }

        // Refuse l'accès si aucune règle d'autorisation ne correspond.
        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access this appointment."
            );
        }

        // Retourne la réponse uniquement après vérification des droits.
        return toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        return toResponse(findAppointmentById(id));
    }

    public AppointmentResponse updateAppointmentStatus(
            Long id,
            AppointmentStatus newStatus
    ){
        Appointment appointment = findAppointmentById(id);

        if (newStatus != AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Use the dedicated cancellation operation to cancel an appointment."
            );
        }

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED){
            throw new IllegalArgumentException(
              "Only scheduled appointments can change status."
            );
        }

        appointment.setStatus(newStatus);

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        return toResponse(updatedAppointment);
    }


    /**
     * Cancels a scheduled appointment.
     * The requester role and patient profile must be resolved
     * by the trusted backend authentication layer.
     */
    public AppointmentResponse cancelAppointment(
            Long id,
            UserRole requesterRole,
            Patient requestingPatient
    ) {
        // Seuls les patients et les administrateurs peuvent annuler.
        if (requesterRole != UserRole.PATIENT
                && requesterRole != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the patient or an admin can cancel an appointment."
            );
        }

        // Vérifie que le profil du patient authentifié est valide.
        if (requesterRole == UserRole.PATIENT
                && (requestingPatient == null
                || requestingPatient.getId() == null)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An authenticated patient profile is required."
            );
        }

        // Recherche le rendez-vous.
        Appointment appointment = findAppointmentById(id);

        // Un patient ne peut annuler que son propre rendez-vous.
        if (requesterRole == UserRole.PATIENT) {
            Long appointmentPatientId = appointment.getPatient().getId();

            if (appointmentPatientId == null
                    || !appointmentPatientId.equals(requestingPatient.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Patients can only cancel their own appointments."
                );
            }
        }

        // Seuls les rendez-vous planifiés peuvent être annulés.
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only scheduled appointments can be cancelled."
            );
        }

        // La limite de deux heures s'applique uniquement aux patients.
        if (requesterRole == UserRole.PATIENT) {
            LocalDateTime appointmentStart = LocalDateTime.of(
                    appointment.getAppointmentDate(),
                    appointment.getStartTime()
            );

            if (appointmentStart.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Patients must cancel at least 2 hours before the appointment."
                );
            }
        }

        // Conserve le rendez-vous en base avec le statut CANCELLED.
        appointment.setStatus(AppointmentStatus.CANCELLED);

        Appointment cancelledAppointment =
                appointmentRepository.save(appointment);

        return toResponse(cancelledAppointment);
    }


    public void deleteAppointment(Long id) {
        Appointment appointment = findAppointmentById(id);
        appointmentRepository.delete(appointment);
    }

    public AppointmentResponse updateAppointment(
            Long id,
            AppointmentRequest request) {

        Appointment appointment = findAppointmentById(id);

        // Vérifier que le rendez-vous peut encore être modifié.
        // Seuls les rendez-vous planifiés peuvent être reprogrammés en V1.
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            // Refuser la modification si le rendez-vous est terminé ou annulé.
            throw new IllegalStateException(
                    "Only scheduled appointments can be updated."
            );
        }

        // Vérifier que la nouvelle date et l'heure du rendez-vous
        // sont toujours dans le futur.
        validateAppointmentNotInPast(
                request.getAppointmentDate(),
                request.getStartTime()
        );

        Doctor doctor = findDoctorById(request.getDoctorId());

        LocalTime startTime = request.getStartTime();


        // Calculer la fin du rendez-vous selon la durée fixe de 30 minutes.
        LocalTime endTime = startTime.plusMinutes(30);

        // Vérifier que le rendez-vous tient dans un créneau autorisé.
        // Cette méthode prend également en compte DAY_OFF et CUSTOM_HOURS.
        validateAppointmentWithinSchedule(
                request.getDoctorId(),
                request.getAppointmentDate(),
                startTime,
                endTime
        );

        // Vérifier les conflits avec les autres rendez-vous.
        // Le rendez-vous portant l'identifiant "id" est exclu.
        validateNoAppointmentConflictForUpdate(
                appointment.getId(),
                request.getDoctorId(),
                appointment.getPatient(),
                request.getAppointmentDate(),
                startTime,
                endTime
        );

        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setType(request.getType());
        appointment.setReason(request.getReason());
        appointment.setNotes(request.getNotes());

        Appointment updatedAppointment =
                appointmentRepository.save(appointment);

        return toResponse(updatedAppointment);
    }
}
