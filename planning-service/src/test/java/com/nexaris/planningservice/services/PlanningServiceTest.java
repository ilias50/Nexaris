package com.nexaris.planningservice.services;

import com.nexaris.planningservice.dto.CreateAgendaEntryRequest;
import com.nexaris.planningservice.dto.MeetingSuggestionRequest;
import com.nexaris.planningservice.dto.MeetingSuggestionResult;
import com.nexaris.planningservice.dto.UpdateUserPreferenceRequest;
import com.nexaris.planningservice.dto.UserPreferenceResponse;
import com.nexaris.planningservice.entities.AgendaEntry;
import com.nexaris.planningservice.entities.PlanningTag;
import com.nexaris.planningservice.entities.UserPlanningPreference;
import com.nexaris.planningservice.repositories.AgendaEntryRepository;
import com.nexaris.planningservice.repositories.PlanningTagRepository;
import com.nexaris.planningservice.repositories.UserPlanningPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningServiceTest {

    @Mock
    private AgendaEntryRepository agendaEntryRepository;

    @Mock
    private UserPlanningPreferenceRepository preferenceRepository;

        @Mock
        private PlanningTagRepository planningTagRepository;

        @Mock
        private NotificationClient notificationClient;

    @InjectMocks
    private PlanningService planningService;

    @Test
    void createManualEntry_ShouldPersistManualLockedEntry() {
        CreateAgendaEntryRequest request = new CreateAgendaEntryRequest(
                10,
                "Sprint planning",
                LocalDateTime.of(2026, 4, 22, 10, 0),
                LocalDateTime.of(2026, 4, 22, 11, 0),
                null,
                "NONE",
                null,
                null,
                false,
                null
        );

        when(agendaEntryRepository.save(any(AgendaEntry.class))).thenAnswer(invocation -> {
            AgendaEntry e = invocation.getArgument(0);
            e.setId(99L);
            return e;
        });

        var response = planningService.createManualEntry(request, 10);

        assertEquals(99L, response.id());
        assertEquals("MANUAL", response.source());
        assertTrue(response.manualLocked());

        ArgumentCaptor<AgendaEntry> captor = ArgumentCaptor.forClass(AgendaEntry.class);
        verify(agendaEntryRepository).save(captor.capture());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    @Test
    void upsertUserPreference_ShouldCreateDefaultedPreference() {
        when(preferenceRepository.findByUserId(7)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(UserPlanningPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPreferenceResponse response = planningService.upsertUserPreference(7, new UpdateUserPreferenceRequest(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                45
        ));

        assertEquals(7, response.userId());
        assertEquals(LocalTime.of(9, 0), response.workDayStart());
        assertEquals(LocalTime.of(18, 0), response.workDayEnd());
        assertEquals(45, response.preferredMeetingBlockMinutes());
    }

    @Test
    void suggestMeetingSlots_ShouldReturnAvailableSuggestions() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 4, 22, 9, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 4, 22, 12, 0);

                PlanningTag blockingTag = new PlanningTag();
                blockingTag.setBlocking(true);

        AgendaEntry blocking = new AgendaEntry();
        blocking.setUserId(1);
        blocking.setStartAt(LocalDateTime.of(2026, 4, 22, 10, 0));
        blocking.setEndAt(LocalDateTime.of(2026, 4, 22, 10, 30));
                blocking.setTags(java.util.Set.of(blockingTag));

        when(agendaEntryRepository.findOverlappingForUsers(List.of(1, 2), windowStart, windowEnd))
                .thenReturn(List.of(blocking));
        when(preferenceRepository.findByUserIdIn(List.of(1, 2))).thenReturn(List.of());

        MeetingSuggestionResult result = planningService.suggestMeetingSlots(new MeetingSuggestionRequest(
                List.of(1, 2),
                30,
                windowStart,
                windowEnd,
                5
        ));

        assertFalse(result.suggestions().isEmpty());
        assertTrue(result.suggestions().stream().noneMatch(s ->
                s.startAt().equals(LocalDateTime.of(2026, 4, 22, 10, 0))
        ));
    }

    @Test
    void suggestMeetingSlots_ShouldFailWhenDurationInvalid() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> planningService.suggestMeetingSlots(new MeetingSuggestionRequest(
                        List.of(1),
                        10,
                        LocalDateTime.of(2026, 4, 22, 9, 0),
                        LocalDateTime.of(2026, 4, 22, 12, 0),
                        5
                )));

        assertEquals(400, ex.getStatusCode().value());
    }

        @Test
        void createTag_ShouldCreateCustomTag() {
                when(planningTagRepository.findByNameIgnoreCase("Deep Work")).thenReturn(Optional.empty());
                when(planningTagRepository.save(any(PlanningTag.class))).thenAnswer(invocation -> {
                        PlanningTag tag = invocation.getArgument(0);
                        tag.setId(12L);
                        return tag;
                });

                var response = planningService.createTag(1, new com.nexaris.planningservice.dto.CreatePlanningTagRequest(
                                "Deep Work",
                                "Bloc concentration",
                                "#334455",
                                true,
                                true
                ));

                assertEquals(12L, response.id());
                assertTrue(response.blocking());
                assertEquals("Deep Work", response.name());
        }
}
