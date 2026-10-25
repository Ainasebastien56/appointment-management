package com.sebastien.appointmentmanagement.config;

import com.sebastien.appointmentmanagement.entity.User;
import com.sebastien.appointmentmanagement.entity.UserRole;
import com.sebastien.appointmentmanagement.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String adminEmail,
            @Value("${app.bootstrap-admin.password:}") String adminPassword) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {

        // Ne rien faire si les paramètres de création ne sont pas configurés.
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        // Normaliser l'adresse pour éviter les doublons liés à la casse.
        String normalizedEmail =
                adminEmail.trim().toLowerCase(Locale.ROOT);

        // Si le compte existe, vérifier qu'il possède déjà le rôle ADMIN.
        var existingUser =
                userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingUser.isPresent()) {
            if (existingUser.get().getRole() != UserRole.ADMIN) {
                throw new IllegalStateException(
                        "The configured bootstrap email belongs to a non-admin user."
                );
            }

            // Ne pas recréer ni modifier un administrateur existant.
            return;
        }

        // Créer le compte avec un mot de passe haché.
        User admin = new User(
                normalizedEmail,
                passwordEncoder.encode(adminPassword),
                UserRole.ADMIN
        );

        userRepository.save(admin);

        System.out.println("Bootstrap administrator account created.");
    }
}