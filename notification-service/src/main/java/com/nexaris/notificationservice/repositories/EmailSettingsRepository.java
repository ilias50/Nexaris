package com.nexaris.notificationservice.repositories;

import com.nexaris.notificationservice.entities.EmailSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailSettingsRepository extends JpaRepository<EmailSettings, Long> {
    Optional<EmailSettings> findTopByOrderByIdAsc();
}
