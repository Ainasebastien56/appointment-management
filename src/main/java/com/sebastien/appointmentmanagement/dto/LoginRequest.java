package com.sebastien.appointmentmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    // Reçoit l'adresse e-mail de l'utilisateur.
    @NotBlank(message= "Email is required.")
    @Email(message = "Email address must be valid.")
    private String email;

    // Reçoit le mot de passe saisi par l'utilisateur.
    @NotBlank(message = "Password is required.")
    private String password;

    // Constructeur sans argument utilisé notamment pour la désérialisation JSON.
    public LoginRequest() {
    }

    // Retourne l'adresse e-mail reçue.
    public String getEmail() {
        return email;
    }

    // Enregistre l'adresse e-mail reçue.
    public void setEmail(String email) {
        this.email = email;
    }

    // Retourne le mot de passe reçu.
    public String getPassword() {
        return password;
    }

    // Enregistre le mot de passe reçu.
    public void setPassword(String password) {
        this.password = password;
    }
}
