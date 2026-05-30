package com.nexaris.authservice.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Entity
@Table(name = "global_settings")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @jakarta.persistence.Id
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // Ex: "registration_enabled"

    @Column(nullable = false)
    private String value; // "true" ou "false"
}