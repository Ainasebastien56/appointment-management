package com.sebastien.appointmentmanagement.repository;

import com.sebastien.appointmentmanagement.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Retrouve le profil patient associé à un compte utilisateur.
    Optional<Patient> findByUser_Id(Long userId);
}
