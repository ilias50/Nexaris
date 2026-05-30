package com.nexaris.authservice.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRegistrationRequest {
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Le format de l'email est invalide") // <--- Validation magique ici
    private String email;

    @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
    private String password;
    private String profileImageUrl;
    private Integer fkCountry;
    private Integer fkLanguage;
    private String countryCode;
    private String languageCode;

    // Constructeur vide (Obligatoire pour Jackson)
    public UserRegistrationRequest() {}

    // Getters et Setters


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public Integer getFkCountry() { return fkCountry; }
    public void setFkCountry(Integer fkCountry) { this.fkCountry = fkCountry; }

    public Integer getFkLanguage() { return fkLanguage; }
    public void setFkLanguage(Integer fkLanguage) { this.fkLanguage = fkLanguage; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }


}