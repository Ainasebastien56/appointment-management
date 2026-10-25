package com.sebastien.appointmentmanagement.dto;

public class AuthResponse {

    // Contient le JWT généré pour l'utilisateur.
    private String accessToken;

    // Indique au frontend le type de jeton à utiliser.
    private String tokenType;

    // Constructeur sans argument utilisé notamment pour la sérialisation.
    public AuthResponse() {
    }

    // Construit la réponse avec le jeton et son type.
    public AuthResponse(String accessToken, String tokenType) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }

    // Retourne le JWT.
    public String getAccessToken() {
        return accessToken;
    }

    // Retourne le type de jeton.
    public String getTokenType() {
        return tokenType;
    }

    // Définit le type de jeton.
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

}
