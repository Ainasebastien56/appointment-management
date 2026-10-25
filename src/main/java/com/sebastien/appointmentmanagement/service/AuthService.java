package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.dto.AuthResponse;
import com.sebastien.appointmentmanagement.dto.LoginRequest;
import com.sebastien.appointmentmanagement.dto.RegistrationRequest;
import com.sebastien.appointmentmanagement.dto.RegistrationResponse;
import com.sebastien.appointmentmanagement.entity.Patient;
import com.sebastien.appointmentmanagement.entity.User;
import com.sebastien.appointmentmanagement.entity.UserRole;
import com.sebastien.appointmentmanagement.repository.PatientRepository;
import com.sebastien.appointmentmanagement.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    // Permet de rechercher un utilisateur par son adresse e-mail.
    private final UserRepository userRepository;

    // Permet de créer le profil patient lié au compte utilisateur.
    private final PatientRepository patientRepository;

    // Permet de comparer un mot de passe reçu avec son empreinte BCrypt.
    private final PasswordEncoder passwordEncoder;

    // Permet de générer un JWT après une authentification réussie.
    private final JwtService jwtService;

    // Spring injecte les trois dépendances nécessaires au service.
    public AuthService(
            UserRepository userRepository,
            PatientRepository patientRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        // Conserve le repository fourni par Spring.
        this.userRepository = userRepository;

        // Conserve le repository des patients.
        this.patientRepository = patientRepository;

        // Conserve l'encodeur de mots de passe.
        this.passwordEncoder = passwordEncoder;

        // Conserve le service de génération des JWT.
        this.jwtService = jwtService;
    }

    // Vérifie les identifiants et retourne un JWT si la connexion réussit.
    public AuthResponse login(LoginRequest request) {

        // Refuse une requête de connexion absente.
        if (request == null) {
            throw new BadCredentialsException(
                    "Invalid email or password."
            );
        }

        // Récupère l'adresse e-mail envoyée par le client.
        String email = request.getEmail();

        // Récupère le mot de passe envoyé par le client.
        String rawPassword = request.getPassword();

        // Vérifie que les deux champs sont présents.
        if (email == null || email.isBlank()
                || rawPassword == null || rawPassword.isBlank()) {
            throw new BadCredentialsException(
                    "Invalid email or password."
            );
        }

        // Recherche le compte correspondant à l'adresse e-mail.
        var user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password."
                        )
                );

        // Récupère l'empreinte du mot de passe enregistrée en base.
        String encodedPassword = user.getPassword();

        // Vérifie le mot de passe sans comparer directement les chaînes.
        if (encodedPassword == null
                || !passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BadCredentialsException(
                    "Invalid email or password."
            );
        }

        // Génère un JWT pour l'utilisateur authentifié.
        String token = jwtService.generateToken(user);

        // Retourne le JWT accompagné de son type Bearer.
        return new AuthResponse(token, "Bearer");
    }

    // Enregistre le compte utilisateur et son profil patient en une transaction.
    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {

        // Normalise l'adresse pour éviter les doublons dus aux majuscules.
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // Refuse l'inscription si cette adresse est déjà utilisée.
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email address is already registered."
            );
        }

        // Hache le mot de passe : le mot de passe original ne sera pas enregistré.
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Crée le compte avec le rôle imposé par le serveur.
        User user = new User(email, encodedPassword, UserRole.PATIENT);

        // Enregistre d'abord le compte afin d'obtenir son identifiant.
        User savedUser = userRepository.save(user);

        // Construit le profil patient lié à ce compte.
        Patient patient = new Patient(
                savedUser,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                request.getDateOfBirth(),
                request.getGender(),
                request.getPhone().trim()
        );

        // Enregistre le profil patient dans la base de données.
        Patient savedPatient = patientRepository.save(patient);

        // Retourne uniquement les informations autorisées, sans mot de passe.
        return new RegistrationResponse(
                savedUser.getId(),
                savedPatient.getId(),
                savedUser.getEmail(),
                savedPatient.getFirstName(),
                savedPatient.getLastName(),
                savedUser.getRole().name()
        );
    }

}
