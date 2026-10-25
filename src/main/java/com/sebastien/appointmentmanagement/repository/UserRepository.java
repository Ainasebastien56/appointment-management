package com.sebastien.appointmentmanagement.repository;

import com.sebastien.appointmentmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Recherche un utilisateur à partir de son adresse e-mail.
    // Optional indique qu'aucun utilisateur ne correspond peut-être à cet e-mail.
    Optional<User> findByEmail(String email);

    // Recherche un utilisateur sans tenir compte des majuscules dans son e-mail.
    Optional<User> findByEmailIgnoreCase(String email);

    // Vérifie si une adresse e-mail existe déjà, sans tenir compte des majuscules.
    boolean existsByEmailIgnoreCase(String email);
}
