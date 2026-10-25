package com.sebastien.appointmentmanagement.repository;

import com.sebastien.appointmentmanagement.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // Retrouve le profil médecin associé à un compte utilisateur.
    Optional<Doctor> findByUser_Id(Long userId);

    // Vérifie si un numéro de licence est déjà utilisé.
    boolean existsByLicenseNumber(String licenseNumber);
}
