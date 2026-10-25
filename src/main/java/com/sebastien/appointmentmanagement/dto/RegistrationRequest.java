package com.sebastien.appointmentmanagement.dto;

import com.sebastien.appointmentmanagement.entity.Gender;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class RegistrationRequest {

    // Adresse e-mail utilisée pour le compte.
    @NotBlank(message = "Email is required.")
    @Email(message = "Email address must be valid.")
    @Size(max = 254, message = "Email must not exceed 254 characters.")
    private String email;

    // Mot de passe qui sera haché avant son enregistrement.
    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must contain at least 8 characters.")
    private String password;

    // Prénom du patient.
    @NotBlank(message = "First name is required.")
    private String firstName;

    // Nom du patient.
    @NotBlank(message = "Last name is required.")
    private String lastName;

    // Date de naissance, qui doit être antérieure à aujourd'hui.
    @NotNull(message = "Date of birth is required.")
    @Past(message = "Date of birth must be in the past.")
    private LocalDate dateOfBirth;

    // Genre correspondant à une valeur de l'enum Gender.
    @NotNull(message = "Gender is required.")
    private Gender gender;

    // Numéro de téléphone du patient.
    @NotBlank(message = "Phone number is required.")
    private String phone;

    // Constructeur sans argument utilisé pour recevoir le JSON.
    public RegistrationRequest() {
    }

    // Retourne l'adresse e-mail.
    public String getEmail() {
        return email;
    }

    // Définit l'adresse e-mail.
    public void setEmail(String email) {
        this.email = email;
    }

    // Retourne le mot de passe reçu.
    public String getPassword() {
        return password;
    }

    // Définit le mot de passe reçu.
    public void setPassword(String password) {
        this.password = password;
    }

    // Retourne le prénom.
    public String getFirstName() {
        return firstName;
    }

    // Définit le prénom.
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    // Retourne le nom.
    public String getLastName() {
        return lastName;
    }

    // Définit le nom.
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    // Retourne la date de naissance.
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    // Définit la date de naissance.
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    // Retourne le genre.
    public Gender getGender() {
        return gender;
    }

    // Définit le genre.
    public void setGender(Gender gender) {
        this.gender = gender;
    }

    // Retourne le numéro de téléphone.
    public String getPhone() {
        return phone;
    }

    // Définit le numéro de téléphone.
    public void setPhone(String phone) {
        this.phone = phone;
    }

}
