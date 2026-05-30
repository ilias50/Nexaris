package com.nexaris.notificationservice.controller;

import com.nexaris.notificationservice.dtos.ChannelPreferenceDto;
import com.nexaris.notificationservice.services.ChannelPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
public class PreferencesController {

    private final ChannelPreferenceService preferenceService;

    public PreferencesController(ChannelPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<ChannelPreferenceDto> getPreferences(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireUserId(userId);
        return ResponseEntity.ok(preferenceService.getPreferences(userId));
    }

    @PutMapping
    public ResponseEntity<ChannelPreferenceDto> savePreferences(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody ChannelPreferenceDto dto) {
        requireUserId(userId);
        return ResponseEntity.ok(preferenceService.savePreferences(userId, dto));
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "X-User-Id manquant");
        }
    }
}
