package com.nexaris.notificationservice.controller;

import com.nexaris.notificationservice.dtos.EmailRequest;
import com.nexaris.notificationservice.dtos.NotificationRequest;
import com.nexaris.notificationservice.dtos.SendNotificationRequest;
import com.nexaris.notificationservice.entities.NotificationEntity;
import com.nexaris.notificationservice.services.MailService;
import com.nexaris.notificationservice.services.NotificationDispatcher;
import com.nexaris.notificationservice.services.NotificationManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private MailService mailService;

    @Mock
    private NotificationManagementService notificationManagementService;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void getAvailableChannels_ShouldReturnOkWithCount() {
        when(notificationDispatcher.getAvailableChannels()).thenReturn(List.of("DISCORD", "EMAIL", "SMS"));

        ResponseEntity<Map<String, Object>> response = notificationController.getAvailableChannels();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().get("count"));
        assertEquals(List.of("DISCORD", "EMAIL", "SMS"), response.getBody().get("available_channels"));
    }

    @Test
    void sendNotification_ShouldReturnCreated() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setChannel("EMAIL");
        request.setRecipient("user@example.com");
        request.setSubject("Sujet");
        request.setMessage("Message");

        NotificationEntity entity = new NotificationEntity();
        entity.setId(1L);

        when(notificationDispatcher.send(any(SendNotificationRequest.class))).thenReturn(entity);

        ResponseEntity<NotificationEntity> response = notificationController.sendNotification(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(entity, response.getBody());
    }

    @Test
    void sendEmail_ShouldReturnCreated() {
        EmailRequest request = new EmailRequest();
        request.setTo("user@example.com");
        request.setSubject("Sujet");
        request.setBody("Body");

        NotificationEntity entity = new NotificationEntity();
        entity.setId(2L);

        when(mailService.sendEmail(any(EmailRequest.class))).thenReturn(entity);

        ResponseEntity<NotificationEntity> response = notificationController.sendEmail(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(entity, response.getBody());
    }

    @Test
    void createNotification_ShouldReturnCreated() {
        NotificationRequest request = new NotificationRequest();
        request.setRecipient("user@example.com");
        request.setType("EMAIL");
        request.setTitle("Sujet");
        request.setMessage("Message");

        NotificationEntity entity = new NotificationEntity();
        entity.setId(3L);

        when(notificationManagementService.createNotification(any(NotificationRequest.class))).thenReturn(entity);

        ResponseEntity<NotificationEntity> response = notificationController.createNotification(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(entity, response.getBody());
    }

    @Test
    void getNotification_ShouldReturnOk() {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(15L);

        when(notificationManagementService.findNotification(15L)).thenReturn(entity);

        ResponseEntity<NotificationEntity> response = notificationController.getNotification(15L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(entity, response.getBody());
    }
}
