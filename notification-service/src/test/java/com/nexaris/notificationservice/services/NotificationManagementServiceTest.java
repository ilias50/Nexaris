package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.dtos.NotificationRequest;
import com.nexaris.notificationservice.entities.NotificationEntity;
import com.nexaris.notificationservice.repositories.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationManagementServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationManagementService notificationManagementService;

    @Test
    void createNotification_ShouldPersistWithCreatedStatus() {
        NotificationRequest request = new NotificationRequest();
        request.setRecipient("user@example.com");
        request.setType("EMAIL");
        request.setTitle("Sujet");
        request.setMessage("Message");

        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> {
            NotificationEntity saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        NotificationEntity result = notificationManagementService.createNotification(request);

        assertEquals(10L, result.getId());
        assertEquals("CREATED", result.getStatus());
        assertNotNull(result.getCreatedAt());

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());
        NotificationEntity persisted = captor.getValue();
        assertEquals("user@example.com", persisted.getRecipient());
        assertEquals("EMAIL", persisted.getType());
        assertEquals("Sujet", persisted.getTitle());
        assertEquals("Message", persisted.getMessage());
        assertEquals("CREATED", persisted.getStatus());
        assertNotNull(persisted.getCreatedAt());
    }

    @Test
    void findNotification_ShouldReturnEntity_WhenIdExists() {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(99L);

        when(notificationRepository.findById(99L)).thenReturn(Optional.of(entity));

        NotificationEntity result = notificationManagementService.findNotification(99L);

        assertSame(entity, result);
    }

    @Test
    void findNotification_ShouldThrow_WhenIdDoesNotExist() {
        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> notificationManagementService.findNotification(404L));

        assertEquals("Notification introuvable avec l'ID 404", ex.getMessage());
    }
}
