package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.dtos.NotificationRequest;
import com.nexaris.notificationservice.entities.NotificationEntity;
import com.nexaris.notificationservice.repositories.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationManagementService {

    private final NotificationRepository notificationRepository;

    public NotificationManagementService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationEntity createNotification(NotificationRequest request) {
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipient(request.getRecipient());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setStatus("CREATED");
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public NotificationEntity findNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable avec l'ID " + id));
    }
}
