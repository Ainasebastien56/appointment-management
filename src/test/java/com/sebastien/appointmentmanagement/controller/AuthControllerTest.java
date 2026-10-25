package com.sebastien.appointmentmanagement.controller;

// Importe la configuration de sécurité de l'application.
import com.sebastien.appointmentmanagement.config.SecurityConfig;

// Importe les DTO utilisés par le contrôleur.
import com.sebastien.appointmentmanagement.dto.LoginRequest;
import com.sebastien.appointmentmanagement.dto.AuthResponse;

// Importe le service que nous allons simuler dans les tests.
import com.sebastien.appointmentmanagement.service.AuthService;

// Importe l'exception utilisée lorsque les identifiants sont incorrects.
import org.springframework.security.authentication.BadCredentialsException;

// Importe les outils de test Spring.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Importe les annotations et outils des tests JUnit et Mockito.
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Importe les outils pour construire les requêtes HTTP de test.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Importe les vérifications des réponses HTTP et du JSON.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Charge uniquement la couche Web nécessaire au test du contrôleur.
@WebMvcTest(AuthController.class)

// Charge les règles de sécurité réellement définies dans l'application.
@Import(SecurityConfig.class)
class AuthControllerTest {

    // Permet d'envoyer des requêtes HTTP simulées sans démarrer un serveur réel.
    @Autowired
    private MockMvc mockMvc;

    // Remplace le service réel par un faux service contrôlé par le test.
    @MockitoBean
    private AuthService authService;

    @Test
    void shouldReturnTokenWhenCredentialsAreValid() throws Exception {

        // Prépare la réponse que le service simulé doit retourner.
        AuthResponse response =
                new AuthResponse("test.jwt.token", "Bearer");

        // Indique au faux service de retourner cette réponse.
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        // Prépare une requête JSON avec des informations de connexion.
        String jsonRequest = """
                {
                    "email": "patient@example.com",
                    "password": "ExamplePassword123!"
                }
                """;

        // Envoie la requête à la route publique de connexion.
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                // Vérifie que la connexion retourne HTTP 200.
                .andExpect(status().isOk())

                // Vérifie que le jeton figure dans la réponse JSON.
                .andExpect(jsonPath("$.accessToken")
                        .value("test.jwt.token"))

                // Vérifie que le type de jeton est Bearer.
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"));

        // Vérifie que le contrôleur a appelé le service.
        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {

        // Simule le refus de la connexion par le service.
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException(
                        "Invalid email or password."
                ));

        // Prépare une requête avec des identifiants quelconques.
        String jsonRequest = """
                {
                    "email": "patient@example.com",
                    "password": "WrongPassword123!"
                }
                """;

        // Envoie la requête et vérifie la réponse de refus.
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                // Vérifie que le contrôleur retourne HTTP 401.
                .andExpect(status().isUnauthorized())

                // Vérifie le message d'erreur générique.
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password."));

        // Vérifie que le service a bien été appelé.
        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {

        // Prépare une requête contenant une adresse e-mail invalide.
        String jsonRequest = """
                {
                    "email": "not-an-email",
                    "password": "ExamplePassword123!"
                }
                """;

        // Envoie la requête et vérifie la validation du DTO.
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest)
                )
                // Vérifie que la validation retourne HTTP 400.
                .andExpect(status().isBadRequest());

        // Vérifie que le service n'a pas été appelé pour cette requête invalide.
        verifyNoInteractions(authService);
    }
}
