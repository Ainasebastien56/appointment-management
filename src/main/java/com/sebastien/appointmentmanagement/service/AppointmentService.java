
package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.dto.AppointmentRequest;
import com.sebastien.appointmentmanagement.dto.AppointmentResponse;
import com.sebastien.appointmentmanagement.dto.AppointmentStatisticsResponse;
import com.sebastien.appointmentmanagement.dto.AvailableSlotResponse;
import com.sebastien.appointmentmanagement.entity.*;
import com.sebastien.appointmentmanagement.exception.AppointmentNotFoundException;
import com.sebastien.appointmentmanagement.repository.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorScheduleExceptionRepository doctorScheduleExceptionRepository;
    private final AppointmentStatusHistoryRepository appointmentStatusHistoryRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            DoctorScheduleRepository doctorScheduleRepository,
            DoctorScheduleExceptionRepository doctorScheduleExceptionRepository,
            AppointmentStatusHistoryRepository appointmentStatusHistoryRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.doctorScheduleExceptionRepository = doctorScheduleExceptionRepository;
        this.appointmentStatusHistoryRepository = appointmentStatusHistoryRepository;
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
        response.setCancellationReason(appointment.getCancellationReason());
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

    // Version sans filtre temporel.
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForPatient(
            Long patientId,
            String doctorName,
            AppointmentStatus status,
            Pageable pageable) {

        // Limite obligatoirement les résultats au patient identifié par le serveur.
        return getAppointmentsScoped(
                patientId, null, doctorName, status, pageable, false, false, false, null
        ).map( response -> {
            if (response.getStatus() == AppointmentStatus.CANCELLED) {
                response.setNotes(null);
            }
            return response;
        });
    }

    // Version avec le filtre Upcoming.
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForPatient(
            Long patientId,
            String doctorName,
            AppointmentStatus status,
            Pageable pageable,
            boolean upcomingOnly) {

        return getAppointmentsScoped(
                patientId,
                null,
                doctorName,
                status,
                pageable,
                upcomingOnly,
                false,
                false,
                null
        ).map(response -> {
            if (response.getStatus() == AppointmentStatus.CANCELLED) {
                response.setNotes(null);
            }
            return response;
        });
    }

    // Version avec les deux filtres temporels.
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForPatient(
            Long patientId,
            String doctorName,
            AppointmentStatus status,
            Pageable pageable,
            boolean upcomingOnly,
            boolean pendingOutcomeOnly) {

        if (upcomingOnly && pendingOutcomeOnly) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Upcoming and pending outcome filters cannot be used together."
            );
        }

        return getAppointmentsScoped(
                patientId,
                null,
                doctorName,
                status,
                pageable,
                upcomingOnly,
                pendingOutcomeOnly,
                false,
                null
        ).map(response -> {
            if (response.getStatus() == AppointmentStatus.CANCELLED) {
                response.setNotes(null);
            }
            return response;
        });
    }

    // Version avec les trois filtres temporels.
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForPatient(
            Long patientId,
            String doctorName,
            AppointmentStatus status,
            Pageable pageable,
            boolean upcomingOnly,
            boolean pendingOutcomeOnly,
            boolean pastOutcomeOnly) {

        validateSingleTemporalFilter(
                upcomingOnly, pendingOutcomeOnly, pastOutcomeOnly
        );

        return getAppointmentsScoped(
                patientId,
                null,
                doctorName,
                status,
                pageable,
                upcomingOnly,
                pendingOutcomeOnly,
                pastOutcomeOnly,
                null
        ).map(response -> {
            if (response.getStatus() == AppointmentStatus.CANCELLED) {
                response.setNotes(null);
            }
            return response;
        });
    }


    // Version sans filtre temporel ni filtre de date.
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForDoctor(
            Long doctorId,
            AppointmentStatus status,
            Pageable pageable
            ) {

        return getAppointmentsForDoctor(
                doctorId, status, pageable, false, false, false, null
        );
    }

    // Retourne les rendez-vous associés au profil du médecin connecté,
    // avec les filtres temporels et un filtre optionnel sur une date précise.
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsForDoctor(
            Long doctorId,
            AppointmentStatus status,
            Pageable pageable,
            boolean upcomingOnly,
            boolean pendingOutcomeOnly,
            boolean pastOutcomeOnly,
            LocalDate date) {

        validateSingleTemporalFilter(
                upcomingOnly, pendingOutcomeOnly, pastOutcomeOnly
        );

        // Le plus proche d'abord pour les rendez-vous à venir et l'agenda
        // d'une journée, le plus récent d'abord pour tout le reste.
        boolean chronological = upcomingOnly || date != null;

        Pageable sortedPageable = withDefaultSort(
                pageable,
                chronological ? Sort.Direction.ASC : Sort.Direction.DESC
        );

        // Limite obligatoirement les résultats au médecin identifié par le serveur.
        return getAppointmentsScoped(
                null,
                doctorId,
                null,
                status,
                sortedPageable,
                upcomingOnly,
                pendingOutcomeOnly,
                pastOutcomeOnly,
                date
        );
    }

    // Applique un tri par date puis heure de début si le client n'en fournit pas.
    private Pageable withDefaultSort(
            Pageable pageable,
            Sort.Direction direction) {

        if (pageable.getSort().isSorted()) {
            return pageable;
        }

        Sort defaultSort = Sort.by(
                new Sort.Order(direction, "appointmentDate"),
                new Sort.Order(direction, "startTime")
        );

        if (pageable.isUnpaged()) {
            return Pageable.unpaged(defaultSort);
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                defaultSort
        );
    }

    // Refuse la combinaison de plusieurs filtres temporels.
    private void validateSingleTemporalFilter(
            boolean upcomingOnly,
            boolean pendingOutcomeOnly,
            boolean pastOutcomeOnly) {

        int activeFilters = (upcomingOnly ? 1 : 0)
                + (pendingOutcomeOnly ? 1 : 0)
                + (pastOutcomeOnly ? 1 : 0);

        if (activeFilters > 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only one temporal filter can be active at a time."
            );
        }
    }

    private Predicate buildTemporalPredicate(
            Root<Appointment> root,
            CriteriaBuilder cb,
            boolean upcomingOnly,
            boolean pendingOutcomeOnly,
            LocalDateTime now
    ) {
        Predicate futureDate = cb.greaterThan(
                root.get("appointmentDate"),
                now.toLocalDate()
        );

        Predicate pastDate = cb.lessThan(
                root.get("appointmentDate"),
                now.toLocalDate()
        );

        Predicate todayAndFutureTime = cb.and(
                cb.equal(root.get("appointmentDate"), now.toLocalDate()),
                cb.greaterThanOrEqualTo(
                        root.get("startTime"),
                        now.toLocalTime()
                )
        );

        Predicate todayAndPastTime = cb.and(
                cb.equal(root.get("appointmentDate"), now.toLocalDate()),
                cb.lessThan(
                        root.get("startTime"),
                        now.toLocalTime()
                )
        );

        if (upcomingOnly) {
            return cb.and(
                    cb.equal(root.get("status"), AppointmentStatus.SCHEDULED),
                    cb.or(futureDate, todayAndFutureTime)
            );
        }

        if (pendingOutcomeOnly) {
            return cb.and(
                    cb.equal(root.get("status"), AppointmentStatus.SCHEDULED),
                    cb.or(pastDate, todayAndPastTime)
            );
        }

        return cb.conjunction();
    }

    private Specification<Appointment> buildPatientStatisticsSpecification(
            Long patientId,
            boolean upcomingOnly,
            boolean pendingOutcomeOnly,
            AppointmentStatus status,
            LocalDateTime now
    ) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Toujours limiter les statistiques au patient concerné.
            predicates.add(criteriaBuilder.equal(
                    root.get("patient").get("id"),
                    patientId
            ));

            // Appliquer éventuellement un filtre de statut.
            if (status != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"),
                        status
                ));
            }

            // Appliquer le filtre temporel si nécessaire.
            if (upcomingOnly || pendingOutcomeOnly) {
                predicates.add(buildTemporalPredicate(
                        root,
                        criteriaBuilder,
                        upcomingOnly,
                        pendingOutcomeOnly,
                        now
                ));
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    @Transactional(readOnly = true)
    public AppointmentStatisticsResponse getPatientAppointmentStatistics(
            Long patientId
    ) {

        // Capturer une seule fois la date et l'heure actuelles.
        LocalDateTime now = LocalDateTime.now();

        // 1. Rendez-vous futurs encore planifiés.
        long scheduledAppointments = appointmentRepository.count(
                buildPatientStatisticsSpecification(
                        patientId,
                        true,
                        false,
                        null,
                        now
                )
        );

        // 2. Rendez-vous passés sans résultat enregistré.
        long pendingOutcomes = appointmentRepository.count(
                buildPatientStatisticsSpecification(
                        patientId,
                        false,
                        true,
                        null,
                        now
                )
        );

        // 3. Tous les rendez-vous annulés.
        long cancelledAppointments = appointmentRepository.count(
                buildPatientStatisticsSpecification(
                        patientId,
                        false,
                        false,
                        AppointmentStatus.CANCELLED,
                        now
                )
        );

        return new AppointmentStatisticsResponse(
                scheduledAppointments,
                pendingOutcomes,
                cancelledAppointments
        );
    }

    // Construit les critères de recherche et convertit les entités en DTO.
    private Page<AppointmentResponse> getAppointmentsScoped(
            Long patientId,
            Long doctorId,
            String doctorName,
            AppointmentStatus status,
            Pageable pageable,
            boolean upcomingOnly,
            boolean pendingOutcomeOnly,
            boolean pastOutcomeOnly,
            LocalDate date) {

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

            if (status != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"),
                        status
                ));
            }

            // Limite les résultats à une date précise si elle est fournie.
            if (date != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("appointmentDate"),
                        date
                ));
            }

            // Applique les filtres temporels uniquement si demandés.
            if (upcomingOnly || pendingOutcomeOnly || pastOutcomeOnly) {

                // Upcoming et Pending Outcome concernent uniquement SCHEDULED.
                if (upcomingOnly || pendingOutcomeOnly) {
                    predicates.add(
                            buildTemporalPredicate(
                                    root,
                                    criteriaBuilder,
                                    upcomingOnly,
                                    pendingOutcomeOnly,
                                    LocalDateTime.now()
                            )
                    );
                }

                if (pastOutcomeOnly) {
                    predicates.add(root.get("status").in(
                            AppointmentStatus.COMPLETED,
                            AppointmentStatus.NO_SHOW
                    ));
                }

            }

            // Recherche partielle sur le prénom et le nom complet du médecin.
            if (doctorName != null && !doctorName.isBlank()) {

                String searchPattern = "%"
                        + doctorName.trim().toLowerCase(Locale.ROOT)
                        + "%";

                Expression<String> fullName = criteriaBuilder.concat(
                        criteriaBuilder.concat(
                                criteriaBuilder.lower(root.get("doctor").get("firstName")),
                                " "
                        ),
                        criteriaBuilder.lower(root.get("doctor").get("lastName"))
                );

                predicates.add(criteriaBuilder.like(fullName, searchPattern));
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

        LocalDate appointmentDate = request.getAppointmentDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = startTime.plusMinutes(30);

        Long selectedDoctorId;
        Doctor doctor;

        if (request.getDoctorId() != null) {
            // Médecin choisi explicitement par le patient.
            selectedDoctorId = request.getDoctorId();
            doctor = findDoctorById(selectedDoctorId);

        }
        else {
        // No Preference : rechercher tous les médecins disponibles.
        List<AvailableSlotResponse> availableSlots =
                getAvailableSlots(null, appointmentDate, patient);

        // Choisir le médecin ayant le moins de rendez-vous
        // planifiés sur la journée, avec un départage par ID.
        AvailableSlotResponse selectedSlot = availableSlots.stream()
                .filter(slot -> slot.startTime().equals(startTime))
                .min(
                        Comparator.comparingLong(
                                (AvailableSlotResponse slot) ->
                                        appointmentRepository
                                                .countByDoctor_IdAndAppointmentDateAndStatus(
                                                        slot.doctorId(),
                                                        appointmentDate,
                                                        AppointmentStatus.SCHEDULED
                                                )
                        ).thenComparing(AvailableSlotResponse::doctorId)
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No doctor is available for the selected date and time."
                ));

        selectedDoctorId = selectedSlot.doctorId();
        doctor = findDoctorById(selectedDoctorId);
    }

    // Utiliser l'identifiant explicite, pas doctor.getId().
        validateAppointmentWithinSchedule(
                selectedDoctorId,
                appointmentDate,
                startTime,
                endTime
        );

        validateNoAppointmentConflict(
                selectedDoctorId,
                patient,
                appointmentDate,
                startTime,
                endTime
        );

        Appointment appointment = new Appointment(
                patient,
                doctor,
                appointmentDate,
                startTime,
                endTime,
                request.getReason(),
                AppointmentStatus.SCHEDULED
        );

        appointment.setType(request.getType());
        appointment.setNotes(request.getNotes());

        Appointment savedAppointment = appointmentRepository.save(appointment);

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

        AppointmentResponse response = toResponse(appointment);

        // Les notes d'un rendez-vous annulé ne sont pas visibles par le patient.
        if (role == UserRole.PATIENT
                && appointment.getStatus() == AppointmentStatus.CANCELLED) {
            response.setNotes(null);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        return toResponse(findAppointmentById(id));
    }

    public AppointmentResponse updateAppointmentStatus(
            Long id,
            AppointmentStatus newStatus,
            User requester,
            Doctor requestingDoctor,
            String changeReason
    ) {
        // Vérifie que l'utilisateur authentifié existe.
        if (requester == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An authenticated user is required."
            );
        }

        // Seuls les médecins et les administrateurs peuvent enregistrer un résultat.
        UserRole role = requester.getRole();

        if (role != UserRole.DOCTOR && role != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only doctors and admins can update appointment status."
            );
        }

        // Les statuts autorisés sont COMPLETED, NO_SHOW et CANCELLED.
        if (newStatus != AppointmentStatus.COMPLETED
                && newStatus != AppointmentStatus.NO_SHOW
                && newStatus != AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only COMPLETED, NO_SHOW or CANCELLED status is allowed."
            );
        }

        boolean isCancellation = newStatus == AppointmentStatus.CANCELLED;

        if (isCancellation) {
            // Les administrateurs annulent via DELETE /api/appointments/{id}.
            if (role != UserRole.DOCTOR) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only doctors can cancel appointments through this endpoint."
                );
            }

            // Le motif est obligatoire : il est communiqué au patient.
            if (changeReason == null || changeReason.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A reason is required to cancel an appointment."
                );
            }
        }

        // Recherche le rendez-vous.
        Appointment appointment = findAppointmentById(id);

        AppointmentStatus previousStatus = appointment.getStatus();

        // Un médecin doit posséder un profil valide.
        if (role == UserRole.DOCTOR
                && (requestingDoctor == null
                || requestingDoctor.getId() == null)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An authenticated doctor profile is required."
            );
        }

        // Un médecin ne peut modifier que ses propres rendez-vous.
        if (role == UserRole.DOCTOR
                && !requestingDoctor.getId().equals(
                appointment.getDoctor().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Doctors can only update their own appointments."
            );
        }

        // Détermine si l'opération est une correction administrative.
        boolean isAdministrativeCorrection =
                previousStatus == AppointmentStatus.COMPLETED
                        || previousStatus == AppointmentStatus.NO_SHOW;

        if (isCancellation) {

            // Seuls les rendez-vous planifiés peuvent être annulés.
            if (previousStatus != AppointmentStatus.SCHEDULED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Only scheduled appointments can be cancelled."
                );
            }

            // Un rendez-vous déjà commencé ne peut plus être annulé.
            LocalDateTime appointmentStart = LocalDateTime.of(
                    appointment.getAppointmentDate(),
                    appointment.getStartTime()
            );

            if (!appointmentStart.isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Appointments that have already started cannot be cancelled."
                );
            }

            // Conserve le motif sur le rendez-vous pour l'afficher au patient.
            changeReason = changeReason.trim();
            appointment.setCancellationReason(changeReason);

        } else if (isAdministrativeCorrection) {

            // Seul un administrateur peut corriger un résultat déjà enregistré.
            if (role != UserRole.ADMIN) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Only an admin can correct an existing appointment result."
                );
            }

            // Une correction doit réellement changer le résultat.
            if (previousStatus == newStatus) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "The appointment already has this status."
                );
            }

            // Une correction administrative nécessite un motif.
            if (changeReason == null || changeReason.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A reason is required for an administrative correction."
                );
            }

        } else if (previousStatus != AppointmentStatus.SCHEDULED) {

            // Un rendez-vous annulé ne peut pas recevoir un résultat.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only scheduled appointments can receive a result."
            );
        }

        // Enregistre le nouveau statut.
        appointment.setStatus(newStatus);

        Appointment updatedAppointment =
                appointmentRepository.save(appointment);

        // Enregistre l'historique du changement.
        AppointmentStatusHistory history =
                new AppointmentStatusHistory(
                        appointment,
                        previousStatus,
                        newStatus,
                        requester,
                        changeReason
                );

        appointmentStatusHistoryRepository.save(history);

        return toResponse(updatedAppointment);
    }

    public AppointmentResponse updateAppointmentNotes(
        Long id,
        String notes,
        User requester,
        Doctor requestingDoctor
    ){
        // Vérifier que l'utilisateur authentifié existe.
        if (requester == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An authenticated user is required."
            );
        }

        UserRole role = requester.getRole();

        // Seuls les médecins et les administrateurs peuvent modifier les notes.
        if (role != UserRole.DOCTOR && role!= UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only doctors and admins can update appointement notes."
            );
        }

        // Vérifier que les notes sont valides.
        if (notes == null || notes.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment notes cannot be blank."
            );
        }

        if (notes.length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment notes cannot exceed 2000 characters."
            );
        }

        // Rechercher le rendez-vous.
        Appointment appointment = findAppointmentById(id);

        // Un médecin doit posséder un profil valide.
        if (role == UserRole.DOCTOR
                && (requestingDoctor == null
                || requestingDoctor.getId() == null)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An authenticated doctor profile is required."
            );
        }

        // Un médecin ne peut modifier que les notes de ses propres rendez-vous.
        if (role == UserRole.DOCTOR
                && !requestingDoctor.getId().equals(
                appointment.getDoctor().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Doctors can only update notes on their own appointments."
            );
        }

        // Un rendez-vous annulé ne peut plus recevoir de notes.
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Notes cannot be updated for cancelled appointments."
            );
        }

        // Modifier uniquement les notes.
        appointment.setNotes(notes.trim());

        // Enregistrer les modifications.
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        return toResponse(updatedAppointment);
    }

    /**
     * Cancels a scheduled appointment.
     * The requester and patient profile must be resolved
     * by the trusted backend authentication layer.
     * The reason is optional and recorded in the status history.
     */
    public AppointmentResponse cancelAppointment(
            Long id,
            User requester,
            Patient requestingPatient,
            String reason
    ) {
        // Vérifie que l'utilisateur authentifié existe.
        if (requester == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An authenticated user is required."
            );
        }

        UserRole requesterRole = requester.getRole();

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

        // Un motif vide est traité comme absent.
        String cancellationReason =
                reason == null || reason.isBlank() ? null : reason.trim();

        // Conserve le rendez-vous en base avec le statut CANCELLED.
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(cancellationReason);

        Appointment cancelledAppointment =
                appointmentRepository.save(appointment);

        // Enregistre l'annulation dans l'historique des statuts.
        appointmentStatusHistoryRepository.save(
                new AppointmentStatusHistory(
                        appointment,
                        AppointmentStatus.SCHEDULED,
                        AppointmentStatus.CANCELLED,
                        requester,
                        cancellationReason
                )
        );

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


    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(
            Long requestedDoctorId,
            LocalDate date,
            Patient patient) {

        if (date == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment date is required."
            );
        }

        if (patient == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An authenticated patient profile is required."
            );
        }

        if (date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot search availability for a past date."
            );
        }

        List<Doctor> doctors;

        if (requestedDoctorId != null) {
            doctors = List.of(findDoctorById(requestedDoctorId));
        } else {
            doctors = doctorRepository.findAll();
        }

        List<AvailableSlotResponse> availableSlots = new ArrayList<>();

        for (Doctor doctor : doctors) {
            Long doctorId = doctor.getId();

            List<DoctorScheduleException> exceptions =
                    doctorScheduleExceptionRepository
                            .findByDoctor_IdAndDate(doctorId, date);

            // Un jour de congé signifie qu'aucun créneau n'est proposé.
            boolean dayOff = exceptions.stream().anyMatch(exception ->
                    exception.getType() == ScheduleExceptionType.DAY_OFF);

            if (dayOff) {
                continue;
            }

            List<LocalTime[]> intervals = new ArrayList<>();

            List<DoctorScheduleException> customHours = exceptions.stream()
                    .filter(exception ->
                            exception.getType()
                                    == ScheduleExceptionType.CUSTOM_HOURS)
                    .toList();

            // Les horaires particuliers remplacent les horaires habituels.
            if (!customHours.isEmpty()) {
                for (DoctorScheduleException exception : customHours) {
                    if (exception.getStartTime() != null
                            && exception.getEndTime() != null
                            && exception.getEndTime()
                            .isAfter(exception.getStartTime())) {

                        intervals.add(new LocalTime[]{
                                exception.getStartTime(),
                                exception.getEndTime()
                        });
                    }
                }
            } else {
                List<DoctorSchedule> schedules =
                        doctorScheduleRepository
                                .findByDoctor_IdAndDayOfWeek(
                                        doctorId,
                                        date.getDayOfWeek()
                                );

                for (DoctorSchedule schedule : schedules) {
                    intervals.add(new LocalTime[]{
                            schedule.getStartTime(),
                            schedule.getEndTime()
                    });
                }
            }

            for (LocalTime[] interval : intervals) {
                LocalTime intervalStart = interval[0];
                LocalTime intervalEnd = interval[1];

                LocalTime startTime = intervalStart;

                while (!startTime.plusMinutes(30).isAfter(intervalEnd)) {
                    LocalTime endTime = startTime.plusMinutes(30);

                    boolean isInPast =
                            LocalDateTime.of(date, startTime)
                                    .isBefore(LocalDateTime.now());

                    boolean doctorHasConflict =
                            appointmentRepository
                                    .existsByDoctor_IdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                            doctorId,
                                            date,
                                            AppointmentStatus.SCHEDULED,
                                            endTime,
                                            startTime
                                    );

                    boolean patientHasConflict =
                            appointmentRepository
                                    .existsByPatientAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                                            patient,
                                            date,
                                            AppointmentStatus.SCHEDULED,
                                            endTime,
                                            startTime
                                    );

                    if (!isInPast
                            && !doctorHasConflict
                            && !patientHasConflict
                            && fitsSlotInterval(
                            intervalStart,
                            intervalEnd,
                            startTime,
                            endTime)) {

                        availableSlots.add(new AvailableSlotResponse(
                                doctorId,
                                fullName(
                                        doctor.getFirstName(),
                                        doctor.getLastName()
                                ),
                                startTime,
                                endTime
                        ));
                    }

                    startTime = startTime.plusMinutes(30);
                }
            }
        }

        availableSlots.sort(
                Comparator.comparing(AvailableSlotResponse::startTime)
                        .thenComparing(AvailableSlotResponse::doctorId)
        );;
        return availableSlots;
    }

}
