package io.github.ckofa.filewatcher.model;

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import io.github.ckofa.filewatcher.model.AppConfigManager.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSenderAdapterTest {

    private static final String TEST_MESSAGE = "Hello, World!";
    private static final String TEST_SUBJECT = "Test Subject";
    private static final String TEST_RECIPIENT = "recipient@example.com";
    private static final String PREFIX_MESSAGE = "PREFIX : ";
    private static final String FINAL_MESSAGE = PREFIX_MESSAGE + TEST_MESSAGE;

    // Get a test logger for our class
    private static final TestLogger logger = TestLoggerFactory.getTestLogger(EmailSenderAdapter.class);

    @Mock
    private AppConfigManager mockConfig;

    @Mock
    private MessagePrefixProvider mockPrefixProvider;

    @Mock
    private EmailNotificationSender mockSender;

    private MockedStatic<EmailNotificationSenderFactory> mockedFactory;

    @BeforeEach
    void setUp() {
        mockedFactory = mockStatic(EmailNotificationSenderFactory.class);
        when(mockPrefixProvider.getMessagePrefix()).thenReturn(PREFIX_MESSAGE);
        logger.clear();
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

        EmailSenderAdapter adapter = new EmailSenderAdapter(mockConfig, mockPrefixProvider);

        // Act
        adapter.sendMessage(TEST_MESSAGE);

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

        EmailSenderAdapter adapter = new EmailSenderAdapter(mockConfig, mockPrefixProvider);

        // Act
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender, never()).sendMessageAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("sendMessage should call sender with correct parameters on success")
    void sendMessage_whenConfigIsValid_shouldCallSender() {
        // Arrange
        mockedFactory.when(() -> EmailNotificationSenderFactory.create(mockConfig))
                .thenReturn(Optional.of(mockSender));

        when(mockConfig.getSettingValue(Settings.MAIL_RECIPIENT_EMAIL)).thenReturn(TEST_RECIPIENT);
        when(mockConfig.getSettingValue(Settings.MAIL_SUBJECT)).thenReturn(TEST_SUBJECT);

        when(mockSender.sendMessageAsync(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        EmailSenderAdapter adapter = new EmailSenderAdapter(mockConfig, mockPrefixProvider);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender, times(1))
                .sendMessageAsync(messageCaptor.capture(), eq(TEST_SUBJECT), eq(TEST_RECIPIENT));
        assertEquals(FINAL_MESSAGE, messageCaptor.getValue());
    }

    @Test
    @DisplayName("sendMessage should handle exceptions from sender")
    void sendMessage_whenSenderThrowsException_shouldLogTheError() {
        // Arrange
        mockedFactory.when(() -> EmailNotificationSenderFactory.create(mockConfig))
                .thenReturn(Optional.of(mockSender));

        when(mockConfig.getSettingValue(Settings.MAIL_RECIPIENT_EMAIL)).thenReturn(TEST_RECIPIENT);
        when(mockConfig.getSettingValue(Settings.MAIL_SUBJECT)).thenReturn(TEST_SUBJECT);

        RuntimeException testException = new RuntimeException("SMTP connection failed");
        when(mockSender.sendMessageAsync(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(testException));

        EmailSenderAdapter adapter = new EmailSenderAdapter(mockConfig, mockPrefixProvider);

        // Act
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender, times(1)).sendMessageAsync(FINAL_MESSAGE, TEST_SUBJECT, TEST_RECIPIENT);

        assertEquals(1, logger.getLoggingEvents().size());
        assertEquals(Level.ERROR, logger.getLoggingEvents().get(0).getLevel());
        assertEquals("Failed to send Email message.", logger.getLoggingEvents().get(0).getMessage());
        //noinspection OptionalGetWithoutIsPresent
        assertSame(testException, logger.getLoggingEvents().get(0).getThrowable().get());
    }
}