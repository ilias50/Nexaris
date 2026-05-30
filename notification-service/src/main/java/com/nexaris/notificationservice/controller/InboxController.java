package com.nexaris.notificationservice.controller;

import com.nexaris.notificationservice.services.InAppNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/notifications/inbox")
public class InboxController {

    private final InAppNotificationService inAppService;

    public InboxController(InAppNotificationService inAppService) {
        this.inAppService = inAppService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getInbox(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireUserId(userId);
        return ResponseEntity.ok(inAppService.getInbox(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireUserId(userId);
        inAppService.markRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireUserId(userId);
        inAppService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "X-User-Id manquant");
        }
    }
}
