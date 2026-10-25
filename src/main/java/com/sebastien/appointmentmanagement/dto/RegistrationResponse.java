package com.sebastien.appointmentmanagement.dto;

// Représente la confirmation de création du compte patient.
public class RegistrationResponse {

    // Identifiant du compte utilisateur créé.
    private Long userId;

    // Identifiant du profil patient créé.
    private Long patientId;

    // Adresse e-mail enregistrée.
    private String email;

    // Prénom du patient.
    private String firstName;

    // Nom du patient.
    private String lastName;

    // Rôle attribué par le serveur.
    private String role;

    // Construit une réponse avec les données du compte créé.
    public RegistrationResponse(
            Long userId,
            Long patientId,
            String email,
            String firstName,
            String lastName,
            String role) {

        this.userId = userId;
        this.patientId = patientId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    // Retourne l'identifiant du compte.
    public Long getUserId() {
        return userId;
    }

    // Retourne l'identifiant du patient.
    public Long getPatientId() {
        return patientId;
    }

    // Retourne l'adresse e-mail.
    public String getEmail() {
        return email;
    }

    // Retourne le prénom.
    public String getFirstName() {
        return firstName;
    }

    // Retourne le nom.
    public String getLastName() {
        return lastName;
    }

    // Retourne le rôle attribué au compte.
    public String getRole() {
        return role;
    }
}