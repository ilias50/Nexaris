package com.nexaris.notificationservice.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class EmailSettingsDto {

    @NotBlank(message = "L'hôte SMTP est obligatoire")
    private String host;

    @Min(value = 1, message = "Le port doit être compris entre 1 et 65535")
    @Max(value = 65535, message = "Le port doit être compris entre 1 et 65535")
    private int port = 587;

    @NotBlank(message = "Le nom d'utilisateur SMTP est obligatoire")
    private String username;

    /** Null or blank = conserver le mot de passe existant */
    private String password;

    @NotBlank(message = "L'adresse expéditeur est obligatoire")
    @Email(message = "Le format de l'adresse expéditeur est invalide")
    private String fromAddress;

    private boolean smtpAuth = true;

    private boolean starttls = true;

    private String sslTrust;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public boolean isSmtpAuth() { return smtpAuth; }
    public void setSmtpAuth(boolean smtpAuth) { this.smtpAuth = smtpAuth; }
    public boolean isStarttls() { return starttls; }
    public void setStarttls(boolean starttls) { this.starttls = starttls; }
    public String getSslTrust() { return sslTrust; }
    public void setSslTrust(String sslTrust) { this.sslTrust = sslTrust; }
}
