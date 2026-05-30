package com.nexaris.authservice.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "countries")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 2)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phone_code", length = 10)
    private String phoneCode;
}