package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.entities.EmailSettings;
import com.nexaris.notificationservice.entities.NotificationEntity;
import com.nexaris.notificationservice.repositories.NotificationRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.server.ResponseStatusException;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private EmailSettingsService emailSettingsService;

    @Mock
    private NotificationRepository notificationRepository;

    private MailService mailService;

    private JavaMailSenderImpl stubSender;

    @BeforeEach
    void setUp() {
        mailService = new MailService(emailSettingsService, notificationRepository);
        when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        stubSender = mock(JavaMailSenderImpl.class);
        MimeMessage stubMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(stubSender.createMimeMessage()).thenReturn(stubMessage);
        when(emailSettingsService.buildMailSender()).thenReturn(stubSender);
    }

    private EmailSettings settingsWithFrom(String fromAddress) {
        EmailSettings s = new EmailSettings();
        s.setHost("smtp.example.com");
        s.setPort(587);
        s.setUsername("user");
        s.setPassword("pass");
        s.setFromAddress(fromAddress);
        return s;
    }

    @Test
    void shouldUseDefaultFromWhenRequestedFromIsNull() throws Exception {
        when(emailSettingsService.getSettings()).thenReturn(settingsWithFrom("no-reply@noreply.local"));

        mailService.send("user@example.com", "Subject", "Message", null, null);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(stubSender).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertNotNull(sent.getFrom());
        assertEquals("no-reply@noreply.local", ((InternetAddress) sent.getFrom()[0]).getAddress());
    }

    @Test
    void shouldUseRequestedFromWhenProvided() throws Exception {
        mailService.send("user@example.com", "Subject", "Message", null, "admin@tenant.com");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(stubSender).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertNotNull(sent.getFrom());
        assertEquals("admin@tenant.com", ((InternetAddress) sent.getFrom()[0]).getAddress());
    }

    @Test
    void shouldMarkNotificationFailedWhenSendThrows() {
        doThrow(new RuntimeException("smtp down")).when(stubSender).send(any(MimeMessage.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> mailService.send("user@example.com", "Subject", "Message", null, null));

        assertEquals(500, ex.getStatusCode().value());

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository, atLeast(2)).save(captor.capture());
        NotificationEntity lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals("FAILED", lastSaved.getStatus());
    }
}
