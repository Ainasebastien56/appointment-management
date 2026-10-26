package com.sebastien.appointmentmanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
public class SecurityConfig {

    /**
     * Configure les règles de sécurité de l'API REST.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {
        return http
                // Autoriser les requêtes provenant du frontend Angular.
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                // L'API utilise des jetons Bearer et non des sessions web.
                // On désactive CSRF pour ce fonctionnement stateless.
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // L'inscription et la connexion seront accessibles sans JWT.
                // Tous les autres endpoints nécessiteront une authentification
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )

                // Activer la vérification des JWT reçus comme Bearer tokens.
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                )

                .build();
    }

    // Crée la clé secrète partagée par le générateur et le vérificateur de JWT.
    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.security.jwt.secret-base64}")
            String encodedSecret) {

        // Décode la représentation Base64 pour obtenir les octets de la clé.
        byte[] keyBytes = Base64.getDecoder().decode(encodedSecret);

        // Refuse les clés de moins de 32 octets pour notre configuration HS256.
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "The JWT signing key must contain at least 32 bytes."
            );
        }

        // Construit la clé cryptographique destinée à HMAC-SHA256.
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Autoriser le frontend Angular en développement.
        configuration.setAllowedOrigins(
                List.of("http://localhost:4200")
        );

        // Autoriser les méthodes HTTP utilisées par l'API.
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        // Autoriser les en-têtes utilisés par Angular et les JWT.
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept")
        );

        // Les requêtes utilisent un Bearer token, pas des cookies.
        configuration.setAllowCredentials(false);

        // Mettre en cache la réponse de pré-vérification pendant une heure.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // Configure le composant qui vérifie les signatures des JWT reçus.
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {

        // Utilise la clé partagée et accepte l'algorithme de signature HS256.
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // Configure le composant qui signera les JWT que notre application créera.
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {

        // Utilise la même clé et le même algorithme que le décodeur.
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    // Déclare l'encodeur de mots de passe que Spring injectera dans les services.
    @Bean
    public PasswordEncoder passwordEncoder() {

        // Utilise BCrypt pour hacher les mots de passe et vérifier les empreintes.
        return new BCryptPasswordEncoder();
    }
}