package com.nexaris.authservice.repositories;

import com.nexaris.authservice.entities.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Integer> {
    // Permet de trouver un pays par son code ISO (ex: "BE")
    Optional<Country> findByCode(String code);
}