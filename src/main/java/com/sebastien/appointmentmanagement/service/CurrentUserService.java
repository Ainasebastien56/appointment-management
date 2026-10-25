package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.entity.Doctor;
import com.sebastien.appointmentmanagement.entity.Patient;
import com.sebastien.appointmentmanagement.entity.User;
import com.sebastien.appointmentmanagement.repository.DoctorRepository;
import com.sebastien.appointmentmanagement.repository.PatientRepository;
import com.sebastien.appointmentmanagement.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    // Recherche les comptes utilisateurs.
    private final UserRepository userRepository;

    // Recherche les profils patients.
    private final PatientRepository patientRepository;

    // Recherche les profils médecins.
    private final DoctorRepository doctorRepository;

    // Spring injecte automatiquement les repositories nécessaires.
    public CurrentUserService(
            UserRepository userRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository) {

        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }


    // Retrouve le compte correspondant au sujet du JWT.
    public User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found."
                ));
    }

    // Retrouve le profil patient du compte connecté.
    public Patient requirePatientProfile(User user) {
        return patientRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "A patient profile is required."
                ));
    }

    // Retrouve le profil médecin du compte connecté.
    public Doctor requireDoctorProfile(User user) {
        return doctorRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "A doctor profile is required."
                ));
    }

    public Patient requirePatientById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient not found."
                ));
    }

}
