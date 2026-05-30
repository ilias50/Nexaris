package com.nexaris.authservice.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "languages")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Language {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 5)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;
}