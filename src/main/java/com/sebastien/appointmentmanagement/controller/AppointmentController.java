
package com.sebastien.appointmentmanagement.controller;

import com.sebastien.appointmentmanagement.dto.*;
import com.sebastien.appointmentmanagement.entity.*;
import com.sebastien.appointmentmanagement.service.AppointmentService;
import com.sebastien.appointmentmanagement.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    // Permet d'identifier l'utilisateur à partir de l'authentification courante.
    private final CurrentUserService currentUserService;

    public AppointmentController(
            AppointmentService appointmentService,
            CurrentUserService currentUserService) {
        this.appointmentService = appointmentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Page<AppointmentResponse> getAppointments(
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) String doctorLastName,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "false") boolean upcomingOnly,
            @RequestParam(defaultValue = "false") boolean pendingOutcomeOnly,
            @RequestParam(defaultValue = "false") boolean pastOutcomeOnly,
            @RequestParam(required = false) LocalDate date,
            Pageable pageable,
            Authentication authentication) {

        // Identifie le compte à partir de l'authentification courante
        User user = currentUserService.requireUser(authentication.getName());

        // Applique les règles de visibilité selon le rôle enregistré du compte.
        switch (user.getRole()){

            case ADMIN:
                //Les administrateurs peuvent consulter la liste générale.
                return appointmentService.getAppointments(
                        doctorLastName, status, pageable);

            case PATIENT:
                // Recherche le véritable profil patitnt lié au compte.
                Patient patient = currentUserService.requirePatientProfile(user);

                // Ne retourne que les rendez-vous appartenant à ce patient.
                return appointmentService.getAppointmentsForPatient(
                        patient.getId(), doctorName, status, pageable, upcomingOnly, pendingOutcomeOnly, pastOutcomeOnly
                );

            case DOCTOR:
                //Recherche le véritable profil médecin lié au compte.
                Doctor doctor = currentUserService.requireDoctorProfile(user);

                // Ne retourne que les rendez-vous associés à ce médecin.
                return appointmentService.getAppointmentsForDoctor(
                        doctor.getId(),
                        status,
                        pageable,
                        upcomingOnly,
                        pendingOutcomeOnly,
                        pastOutcomeOnly,
                        date
                );

            default:
                // Refuse tout Rôle qui n'est pas prévu pour cette fonctionnalité.
                throw  new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "This account cannot access appointments."
                );
        }
    }

    @GetMapping("/my-statistics")
    public AppointmentStatisticsResponse getMyAppointmentStatistics(
            Authentication authentication
    ) {
        // Récupérer l'utilisateur authentifié.
        User user = currentUserService.requireUser(authentication.getName());

        // Cet endpoint est réservé aux patients.
        if (user.getRole() != UserRole.PATIENT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only patients can access their appointment statistics."
            );
        }

        // Récupérer le profil patient associé au compte connecté.
        Patient patient = currentUserService.requirePatientProfile(user);

        // Calculer les statistiques de ce patient.
        return appointmentService.getPatientAppointmentStatistics(
                patient.getId()
        );
    }

    @GetMapping("/my-doctor-statistics")
    public DoctorAppointmentStatisticsResponse getMyDoctorAppointmentStatistics(
            Authentication authentication
    ) {
        // Récupérer l'utilisateur authentifié.
        User user = currentUserService.requireUser(authentication.getName());

        // Cet endpoint est réservé aux médecins.
        if (user.getRole() != UserRole.DOCTOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only doctors can access doctor appointment statistics."
            );
        }

        // Récupérer le profil médecin associé au compte connecté.
        Doctor doctor = currentUserService.requireDoctorProfile(user);

        // Calculer les statistiques de ce médecin uniquement.
        return appointmentService.getDoctorAppointmentStatistics(
                doctor.getId()
        );
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse>  createAppointment(
            @Valid @RequestBody AppointmentRequest appointmentRequest,
            Authentication authentication) {

        // Retrouve le compte à partir de l'e-mail contenu dans le sujet du JWT.
        User user = currentUserService.requireUser(authentication.getName());

        // Seul un compte PATIENT peut réserver un rendez-vous avec cette route.
        if (user.getRole() != UserRole.PATIENT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only patients can create appointments."
            );
        }

        // Retrouve le profil patient associé à ce compte dans la base de données.
        Patient patient = currentUserService.requirePatientProfile(user);

        // Réutilise les règles métier existantes pour créer le rendez-vous.
        AppointmentResponse response = appointmentService.createAppointment(
                appointmentRequest,
                patient
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Récupère un rendez-vous uniquement si l'utilisateur y a accès.
    @GetMapping("/{id}")
    public AppointmentResponse getAppointmentById(
            @PathVariable Long id,
            Authentication authentication) {

        // Identifie le compte à partir de l'authentification courante.
        User user = currentUserService.requireUser(authentication.getName());

        //Un administrateur peut consulter tout rendez-vous.
        if (user.getRole() == UserRole.ADMIN){
            return appointmentService.getAppointmentById(id);
        }

        // Pour un patient, utilise uniquement son profil chargé depuis la base.
        if (user.getRole() == UserRole.PATIENT){
            Patient patient = currentUserService.requirePatientProfile(user);

            return appointmentService.getAppointmentByIdForUser(
                    id,
                    user.getRole(),
                    patient.getId()
            );
        }

        // Pour un médecin, utilise uniquement son profil chargé depuis la base.
        if (user.getRole() == UserRole.DOCTOR) {
            Doctor doctor = currentUserService.requireDoctorProfile(user);

            return appointmentService.getAppointmentByIdForUser(
                    id, user.getRole(), doctor.getId()
            );
        }

        // Refuse tout autre rôle.
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "This account cannot access appointments."
        );
    }


    @GetMapping("/available-slots")
    public List<AvailableSlotResponse> getAvailableSlots(
            @RequestParam(required = false) Long doctorId,
            @RequestParam LocalDate date,
            Authentication authentication) {

        User user = currentUserService.requireUser(authentication.getName());

        if (user.getRole() != UserRole.PATIENT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only patients can search appointment availability."
            );
        }

        Patient patient = currentUserService.requirePatientProfile(user);

        return appointmentService.getAvailableSlots(
                doctorId,
                date,
                patient
        );
    }

    @PatchMapping("/{id}/status")
    public AppointmentResponse updateAppointmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request,
            Authentication authentication
    ) {
        // Identifie l'utilisateur authentifié.
        User user = currentUserService.requireUser(authentication.getName());

        Doctor doctor = null;

        // Récupère le véritable profil du médecin si nécessaire.
        if (user.getRole() == UserRole.DOCTOR) {
            doctor = currentUserService.requireDoctorProfile(user);
        } else if (user.getRole() != UserRole.ADMIN) {

            // Les patients ne peuvent pas modifier le résultat d'un rendez-vous.
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only doctors and administrators can update appointment status."
            );
        }

        // Délègue les vérifications métier et l'enregistrement de l'historique au service.
        return appointmentService.updateAppointmentStatus(
                id,
                request.getStatus(),
                user,
                doctor,
                request.getChangeReason()
        );
    }


    @PatchMapping("/{id}/notes")
    public AppointmentResponse updateAppointmentNotes(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentNotesRequest request,
            Authentication authentication
    ) {
        User user = currentUserService.requireUser(authentication.getName());

        Doctor doctor = null;

        if (user.getRole() == UserRole.DOCTOR) {
            doctor = currentUserService.requireDoctorProfile(user);
        } else if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only doctors and administrators can update appointment notes."
            );
        }

        return appointmentService.updateAppointmentNotes(
                id,
                request.getNotes(),
                user,
                doctor
        );
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelAppointment(
            @PathVariable Long id,
            // Corps facultatif : permet de fournir un motif d'annulation.
            @Valid @RequestBody(required = false) CancelAppointmentRequest request,
            Authentication authentication
    ) {
        // Récupère le compte connecté depuis le JWT.
        User user = currentUserService.requireUser(authentication.getName());

        Patient patient = null;

        // Un patient doit utiliser son propre profil enregistré.
        if (user.getRole() == UserRole.PATIENT) {
            patient = currentUserService.requirePatientProfile(user);
        } else if (user.getRole() != UserRole.ADMIN) {
            // Les médecins ne peuvent pas annuler via cet endpoint.
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only patients or administrators can cancel appointments."
            );
        }

        // Applique les règles métier : propriétaire, statut et délai de deux heures.
        appointmentService.cancelAppointment(
                id,
                user,
                patient,
                request == null ? null : request.getReason()
        );
    }

    @PutMapping("/{id}")
    public AppointmentResponse updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest appointmentRequest,
            Authentication authentication) {

        // Récupère le compte connecté depuis le JWT.
        User user = currentUserService.requireUser(authentication.getName());

        // L'administrateur peut modifier n'importe quel rendez-vous.
        if (user.getRole() == UserRole.ADMIN) {
            return appointmentService.updateAppointment(id, appointmentRequest);
        }

        // Un patient ne peut modifier que ses propres rendez-vous.
        if (user.getRole() == UserRole.PATIENT) {
            Patient patient = currentUserService.requirePatientProfile(user);

            // Vérifie le propriétaire avant d'autoriser la modification.
            appointmentService.getAppointmentByIdForUser(
                    id,
                    UserRole.PATIENT,
                    patient.getId()
            );

            return appointmentService.updateAppointment(id, appointmentRequest);
        }

        // Les autres rôles ne peuvent pas modifier les rendez-vous.
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the appointment owner or an administrator can update appointments."
        );
    }
}
