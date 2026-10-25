package com.sebastien.appointmentmanagement.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String firstName;

    private String lastName;

    private String profilePhotoUrl;

    private String specialty;

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    private String phone;

    private LocalDate practiceStartDate;

    @Column(length = 2000)
    private String bio;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected Doctor(){
    }

    public Doctor(
            User user,
            String firstName,
            String lastName,
            String specialty,
            String licenseNumber,
            String phone,
            LocalDate practiceStartDate
    ) {
        this.user = user;
        this.firstName = firstName;
        this.lastName = lastName;
        this.specialty = specialty;
        this.licenseNumber = licenseNumber;
        this.phone = phone;
        this.practiceStartDate = practiceStartDate;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public String getSpecialty() {
        return specialty;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getPracticeStartDate() {
        return practiceStartDate;
    }

    public String getBio() {
        return bio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
