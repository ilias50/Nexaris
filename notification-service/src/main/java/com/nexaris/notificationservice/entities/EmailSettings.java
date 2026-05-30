package com.nexaris.notificationservice.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_settings")
public class EmailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port = 587;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "from_address", nullable = false)
    private String fromAddress = "no-reply@noreply.local";

    @Column(name = "smtp_auth", nullable = false)
    private boolean smtpAuth = true;

    @Column(nullable = false)
    private boolean starttls = true;

    @Column(name = "ssl_trust")
    private String sslTrust;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
