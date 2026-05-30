package com.nexaris.authservice.repositories;

import com.nexaris.authservice.entities.GlobalSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GlobalSettingRepository extends JpaRepository<GlobalSetting, Integer> {

    // On cherche le réglage par son nom unique
    Optional<GlobalSetting> findByName(String name);

    // Méthode utilitaire pour ton service
    default boolean isRegistrationEnabled() {
        return findByName("registration_enabled")
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(true); // Par défaut true si la ligne n'existe pas encore
    }
}