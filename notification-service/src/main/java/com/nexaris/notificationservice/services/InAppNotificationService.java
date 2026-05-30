package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.dtos.InAppNotificationDto;
import com.nexaris.notificationservice.entities.InAppNotification;
import com.nexaris.notificationservice.repositories.InAppNotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InAppNotificationService {

    private static final int MAX_INBOX_SIZE = 30;

    private final InAppNotificationRepository repository;

    public InAppNotificationService(InAppNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InAppNotification create(Long userId, String title, String message, String link) {
        InAppNotification n = new InAppNotification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        return repository.save(n);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInbox(Long userId) {
        List<InAppNotificationDto> items = repository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_INBOX_SIZE))
                .stream()
                .map(this::toDto)
                .toList();
        long unreadCount = repository.countByUserIdAndReadFalse(userId);
        return Map.of("notifications", items, "unreadCount", unreadCount);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        InAppNotification n = repository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable"));
        if (!n.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        n.setRead(true);
        repository.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        repository.markAllReadForUser(userId);
    }

    private InAppNotificationDto toDto(InAppNotification n) {
        InAppNotificationDto dto = new InAppNotificationDto();
        dto.setId(n.getId());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setLink(n.getLink());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
