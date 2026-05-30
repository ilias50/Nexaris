package com.nexaris.notificationservice.repositories;

import com.nexaris.notificationservice.entities.NotificationChannelPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationChannelPreferenceRepository extends JpaRepository<NotificationChannelPreference, Long> {

    Optional<NotificationChannelPreference> findByUserId(Long userId);
}
