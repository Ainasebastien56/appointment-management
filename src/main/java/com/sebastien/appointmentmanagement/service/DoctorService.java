package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.dto.DoctorCreateRequest;
import com.sebastien.appointmentmanagement.dto.DoctorResponse;
import com.sebastien.appointmentmanagement.entity.Doctor;
import com.sebastien.appointmentmanagement.entity.User;
import com.sebastien.appointmentmanagement.entity.UserRole;
import com.sebastien.appointmentmanagement.repository.DoctorRepository;
import com.sebastien.appointmentmanagement.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class DoctorService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(
            UserRepository userRepository,
            DoctorRepository doctorRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public DoctorResponse createDoctor(
            DoctorCreateRequest request,
            String authenticatedEmail
    ) {
        // Vérifie le rôle réel du compte connecté dans la base de données.
        User currentUser = userRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found."
                ));

        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can create doctor accounts."
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Refuse une adresse e-mail déjà utilisée.
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email address already exists."
            );
        }

        String licenseNumber = request.getLicenseNumber().trim();

        // Refuse un numéro de licence déjà utilisé.
        if (doctorRepository.existsByLicenseNumber(licenseNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A doctor with this license number already exists."
            );
        }

        // Crée le compte utilisateur avec un mot de passe haché.
        User doctorUser = new User(
                email,
                passwordEncoder.encode(request.getPassword()),
                UserRole.DOCTOR
        );

        User savedUser = userRepository.save(doctorUser);

        // Crée le profil professionnel associé au compte.
        Doctor doctor = new Doctor(
                savedUser,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                request.getSpecialty().trim(),
                licenseNumber,
                request.getPhone(),
                request.getPracticeStartDate()
        );

        Doctor savedDoctor = doctorRepository.save(doctor);

        // Retourne uniquement les données autorisées.
        return new DoctorResponse(
                savedDoctor.getId(),
                savedUser.getId(),
                savedUser.getEmail(),
                savedDoctor.getFirstName(),
                savedDoctor.getLastName(),
                savedDoctor.getSpecialty(),
                savedDoctor.getLicenseNumber(),
                savedDoctor.getPhone(),
                savedDoctor.getPracticeStartDate()
        );
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(doctor -> new DoctorResponse(
                        doctor.getId(),
                        doctor.getUser().getId(),
                        doctor.getUser().getEmail(),
                        doctor.getFirstName(),
                        doctor.getLastName(),
                        doctor.getSpecialty(),
                        doctor.getLicenseNumber(),
                        doctor.getPhone(),
                        doctor.getPracticeStartDate()
                ))
                .toList();
    }
}