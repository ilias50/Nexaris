package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.dtos.SendNotificationRequest;
import com.nexaris.notificationservice.entities.NotificationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel smsChannel;

    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(emailChannel.getChannelType()).thenReturn("EMAIL");
        when(smsChannel.getChannelType()).thenReturn("SMS");
        dispatcher = new NotificationDispatcher(List.of(emailChannel, smsChannel));
    }

    @Test
    void shouldReturnSortedAvailableChannels() {
        List<String> channels = dispatcher.getAvailableChannels();
        assertEquals(List.of("EMAIL", "SMS"), channels);
    }

    @Test
    void shouldDelegateSendToSelectedChannel() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setChannel("email");
        request.setRecipient("user@example.com");
        request.setSubject("Subject");
        request.setMessage("Message");
        request.setHtmlContent("<p>Message</p>");

        NotificationEntity expected = new NotificationEntity();
        expected.setStatus("SENT");

        when(emailChannel.send(eq("user@example.com"), eq("Subject"), eq("Message"), eq("<p>Message</p>")))
                .thenReturn(expected);

        NotificationEntity actual = dispatcher.send(request);

        assertEquals("SENT", actual.getStatus());
    }

    @Test
    void shouldThrowBadRequestForUnknownChannel() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setChannel("slack");
        request.setRecipient("user@example.com");
        request.setSubject("Subject");
        request.setMessage("Message");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> dispatcher.send(request));

        assertEquals(400, ex.getStatusCode().value());
    }
}
