package com.sebastien.appointmentmanagement.controller;

import com.sebastien.appointmentmanagement.dto.AuthResponse;
import com.sebastien.appointmentmanagement.dto.LoginRequest;
import com.sebastien.appointmentmanagement.dto.RegistrationRequest;
import com.sebastien.appointmentmanagement.dto.RegistrationResponse;
import com.sebastien.appointmentmanagement.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
// Définit le préfixe commun des routes d'authentification.
@RequestMapping("/api/auth")
public class AuthController {

    // Service responsable de la logique d'authentification.
    private final AuthService authService;

    // Spring injecte le service d'authentification dans le contrôleur.
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Déclare la route POST /api/auth/login.
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
            ){
        try {
            // Vérifie les identifiants et génère le JWT si la connexion réussit.
            AuthResponse response = authService.login(request);

            // Retourne le JWT avec le statut HTTP 200.
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException exception){
            // Retourne une erreur générique sans révéler si l'e-mail existe.
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                       "message",
                       "Invalid email or password."
                    ));
        }
    }

    // Expose la route publique POST /api/auth/register.
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            // Convertit le JSON reçu et valide les champs de la requête.
            @Valid @RequestBody RegistrationRequest request) {

        // Appelle le service qui crée le compte et son profil patient.
        RegistrationResponse response = authService.register(request);

        // Retourne HTTP 201 Created avec les informations du compte créé.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
