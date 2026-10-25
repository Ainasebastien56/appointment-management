package com.sebastien.appointmentmanagement.service;

import com.sebastien.appointmentmanagement.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
public class JwtService {

    // Identifie l'application qui émet le jeton.
    private static final String TOKEN_ISSUER = "appointment-management";

    // Le composant qui signera les JWT.
    private final JwtEncoder jwtEncoder;

    // La durée de validité du jeton, exprimée en minutes.
    private final long expirationMinutes;

    // Spring injecte automatiquement le générateur JWT et la durée configurée.
    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.expiration-minutes:60}")
            long expirationMinutes) {

        // Refuse une durée nulle ou négative.
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException(
                    "JWT expiration must be greater than zero."
            );
        }
        // Conserve le générateur JWT fourni par Spring.
        this.jwtEncoder = jwtEncoder;

        // Conserve la durée de validité choisie.
        this.expirationMinutes = expirationMinutes;

    }

    // Génère un JWT pour l'utilisateur reçu en paramètre.
    public String generateToken(User user) {

        // Vérifie que l'utilisateur existe dans l'appel à cette méthode.
        Objects.requireNonNull(user, "User must not be null.");

        // Vérifie que les informations nécessaires à l'identification existent.
        if (user.getEmail() == null || user.getRole() == null) {
            throw new IllegalArgumentException(
                    "User email and role are required to generate a JWT."
            );
        }

        // Enregistre le moment de création du jeton.
        Instant issuedAt = Instant.now();

        // Calcule la date à laquelle le jeton expirera.
        Instant expiresAt = issuedAt.plus(
                expirationMinutes,
                ChronoUnit.MINUTES
        );

        // Prépare les informations qui seront contenues dans le JWT.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                // Indique l'application qui a émis le jeton.
                .issuer(TOKEN_ISSUER)

                // Utilise l'adresse e-mail comme identifiant du sujet.
                .subject(user.getEmail())

                // Enregistre la date de création du jeton.
                .issuedAt(issuedAt)

                // Enregistre la date d'expiration du jeton.
                .expiresAt(expiresAt)

                // Enregistre le rôle de l'utilisateur dans une revendication.
                .claim("role", user.getRole().name())

                // Termine la construction des informations du JWT.
                .build();

        // Prépare l'en-tête en choisissant l'algorithme de signature HS256.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        // Signe le JWT et retourne sa représentation textuelle.
        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}
