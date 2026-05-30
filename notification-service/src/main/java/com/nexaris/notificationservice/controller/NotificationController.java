package com.nexaris.notificationservice.controller;

import com.nexaris.notificationservice.dtos.BulkNotificationRequest;
import com.nexaris.notificationservice.dtos.EmailRequest;
import com.nexaris.notificationservice.dtos.NotificationRequest;
import com.nexaris.notificationservice.dtos.SendNotificationRequest;
import com.nexaris.notificationservice.entities.NotificationEntity;
import com.nexaris.notificationservice.services.MailService;
import com.nexaris.notificationservice.services.NotificationDispatcher;
import com.nexaris.notificationservice.services.NotificationManagementService;
import com.nexaris.notificationservice.services.UserNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final MailService mailService;
    private final NotificationManagementService notificationService;
    private final NotificationDispatcher notificationDispatcher;
    private final UserNotificationService userNotificationService;

    public NotificationController(MailService mailService,
                                  NotificationManagementService notificationService,
                                  NotificationDispatcher notificationDispatcher,
                                  UserNotificationService userNotificationService) {
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.notificationDispatcher = notificationDispatcher;
        this.userNotificationService = userNotificationService;
    }

    @GetMapping("/channels")
    public ResponseEntity<Map<String, Object>> getAvailableChannels() {
        List<String> channels = notificationDispatcher.getAvailableChannels();
        return ResponseEntity.ok(Map.of(
                "available_channels", channels,
                "count", channels.size()
        ));
    }

    @GetMapping("/event-types")
    public ResponseEntity<Map<String, Object>> getAvailableNotificationTypes() {
        List<String> eventTypes = userNotificationService.getAvailableNotificationTypes();
        return ResponseEntity.ok(Map.of(
            "available_event_types", eventTypes,
            "count", eventTypes.size()
        ));
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationEntity> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        NotificationEntity result = notificationDispatcher.send(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/send-bulk")
    public ResponseEntity<Void> sendBulkNotification(@Valid @RequestBody BulkNotificationRequest request) {
        userNotificationService.sendBulk(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email")
    public ResponseEntity<NotificationEntity> sendEmail(@Valid @RequestBody EmailRequest request) {
        NotificationEntity result = mailService.sendEmail(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping
    public ResponseEntity<NotificationEntity> createNotification(@Valid @RequestBody NotificationRequest request) {
        NotificationEntity result = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationEntity> getNotification(@PathVariable Long id) {
        NotificationEntity result = notificationService.findNotification(id);
        return ResponseEntity.ok(result);
    }
}
