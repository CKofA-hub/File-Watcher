package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

import io.github.ckofa.filewatcher.model.AppConfigManager.Settings;

@ExtendWith(MockitoExtension.class)
class EmailSenderAdapterTest {

    @Mock
    private AppConfigManager mockConfig;

    @Mock
    private EmailNotificationSender mockSender;

    private MockedStatic<EmailNotificationSenderFactory> mockedFactory;

    @BeforeEach
    void setUp() {
        mockedFactory = mockStatic(EmailNotificationSenderFactory.class);
    }

    @AfterEach
    void tearDown() {
        mockedFactory.close();
    }

    @Test
    @DisplayName("sendMessage should do nothing if sender is not configured")
    void sendMessage_whenSenderIsNotPresent_shouldDoNothing() {
        // Arrange
        mockedFactory.when(() -> EmailNotificationSenderFactory.create(mockConfig))
                .thenReturn(Optional.empty());

        EmailSenderAdapter adapter = new EmailSenderAdapter(mockConfig);

        // Act
        adapter.sendMessage("Test message");

        // Assert
        verify(mockSender, never()).sendMessageAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendMessage should not send email if recipient is blank")
    void sendMessage_whenRecipientIsBlank_shouldNotSend() {
        // Arrange
        mockedFactory.when(() -> EmailNotificationSenderFactory.create(mockConfig))
                .thenReturn(Optional.of(mockSender));

        when(mockConfig.getSettingValue(Settings.MAIL_RECIPIENT_EMAIL)).thenReturn("  ");

        EmailSenderAdapter adapter = new EmailSenderAdapter(mockConfig);

        // Act
        adapter.sendMessage("Test message");

        // Assert
        verify(mockSender, never()).sendMessageAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendMessage should call sender with correct parameters on success")
    void sendMessage_whenConfigIsValid_shouldCallSender() {
        // Arrange
        String testMessage = "Hello from test!";
        String testSubject = "Test Subject";
        String testRecipient = "recipient@example.com";

        mockedFactory.when(() -> EmailNotificationSenderFactory.create(mockConfig))
                .thenReturn(Optional.of(mockSender));

        when(mockConfig.getSettingValue(Settings.MAIL_RECIPIENT_EMAIL)).thenReturn(testRecipient);
        when(mockConfig.getSettingValue(Settings.MAIL_SUBJECT)).thenReturn(testSubject);

        when(mockSender.sendMessageAsync(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        EmailSenderAdapter adapter = new EmailSenderAdapter(mockConfig);

        // Act
        adapter.sendMessage(testMessage);

        // Assert
        verify(mockSender, times(1)).sendMessageAsync(testMessage, testSubject, testRecipient);
    }

    @Test
    @DisplayName("sendMessage should handle exceptions from sender")
    void sendMessage_whenSenderThrowsException_shouldHandleGracefully() {
        // Arrange
        String testMessage = "Hello from test!";
        String testSubject = "Test Subject";
        String testRecipient = "recipient@example.com";

        mockedFactory.when(() -> EmailNotificationSenderFactory.create(mockConfig))
                .thenReturn(Optional.of(mockSender));

        when(mockConfig.getSettingValue(Settings.MAIL_RECIPIENT_EMAIL)).thenReturn(testRecipient);
        when(mockConfig.getSettingValue(Settings.MAIL_SUBJECT)).thenReturn(testSubject);

        when(mockSender.sendMessageAsync(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("SMTP connection failed")));

        EmailSenderAdapter adapter = new EmailSenderAdapter(mockConfig);

        // Act
        adapter.sendMessage(testMessage);

        // Assert
        verify(mockSender, times(1)).sendMessageAsync(testMessage, testSubject, testRecipient);
    }

}