package io.github.ckofa.filewatcher.model;

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import com.pengrad.telegrambot.response.SendResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import io.github.ckofa.filewatcher.model.AppConfigManager.Settings;
import org.slf4j.event.Level;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
class TelegramSenderAdapterTest {

    private static final long VALID_CHAT_ID = 123456789L;
    private static final String TEST_MESSAGE = "Hello, World!";
    private static final String PREFIX_MESSAGE = "PREFIX : ";
    private static final String FINAL_MESSAGE = PREFIX_MESSAGE + TEST_MESSAGE;

    private static final TestLogger logger = TestLoggerFactory.getTestLogger(TelegramSenderAdapter.class);

    @Mock
    private AppConfigManager mockAppConfigManager;

    @Mock
    private MessagePrefixProvider mockPrefixProvider;

    @Mock
    private TelegramNotificationSender mockSender;

    @Mock
    private SendResponse mockSendResponse;

    private MockedStatic<TelegramNotificationSenderFactory> mockedFactory;

    @BeforeEach
    void setUp() {
        mockedFactory = mockStatic(TelegramNotificationSenderFactory.class);
        when(mockPrefixProvider.getMessagePrefix()).thenReturn(PREFIX_MESSAGE);
        logger.clear();
    }

    @AfterEach
    void tearDown() {
        mockedFactory.close();
    }

    @Test
    @DisplayName("Should successfully send the message if the configuration is correct")
    void sendMessage_whenConfigValid_shouldSuccess() {
        // Arrange
        when(mockAppConfigManager.getLongSettingValue(Settings.TELEGRAM_CHAT_ID)).thenReturn(VALID_CHAT_ID);
        mockedFactory.when(() -> TelegramNotificationSenderFactory.create(mockAppConfigManager))
                .thenReturn(Optional.of(mockSender));

        CompletableFuture<SendResponse> future = CompletableFuture.completedFuture(mockSendResponse);
        when(mockSender.sendMessageAsync(VALID_CHAT_ID, FINAL_MESSAGE)).thenReturn(future);
        when(mockSendResponse.isOk()).thenReturn(true);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager, mockPrefixProvider);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender).sendMessageAsync(eq(VALID_CHAT_ID), messageCaptor.capture());
        assertEquals(FINAL_MESSAGE, messageCaptor.getValue());
        verify(mockSendResponse).isOk();
    }

    @Test
    @DisplayName("Should not send message when sender cannot be created")
    void sendMessage_whenSenderNotCreated_shouldNotSendMessage() {
        // Arrange
        when(mockAppConfigManager.getLongSettingValue(Settings.TELEGRAM_CHAT_ID)).thenReturn(VALID_CHAT_ID);
        mockedFactory.when(() -> TelegramNotificationSenderFactory.create(mockAppConfigManager))
                .thenReturn(Optional.empty());

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager, mockPrefixProvider);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender, never()).sendMessageAsync(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should not send message when chat ID is not configured")
    void sendMessage_whenChatIdIsMissing_shouldNotSendMessage() {
        // Arrange
        when(mockAppConfigManager.getLongSettingValue(Settings.TELEGRAM_CHAT_ID))
                .thenThrow(new IllegalArgumentException("Setting not found"));

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager, mockPrefixProvider);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockAppConfigManager).getLongSettingValue(Settings.TELEGRAM_CHAT_ID);
        verify(mockSender, never()).sendMessageAsync(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should log error when Telegram API returns an error response")
    void sendMessage_whenApiReturnsError_shouldHandleError() {
        // Arrange
        when(mockAppConfigManager.getLongSettingValue(Settings.TELEGRAM_CHAT_ID)).thenReturn(VALID_CHAT_ID);
        mockedFactory.when(() -> TelegramNotificationSenderFactory.create(mockAppConfigManager))
                .thenReturn(Optional.of(mockSender));

        CompletableFuture<SendResponse> future = CompletableFuture.completedFuture(mockSendResponse);
        when(mockSender.sendMessageAsync(VALID_CHAT_ID, FINAL_MESSAGE)).thenReturn(future);

        when(mockSendResponse.isOk()).thenReturn(false);
        when(mockSendResponse.errorCode()).thenReturn(400);
        when(mockSendResponse.description()).thenReturn("Bad Request: chat not found");

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager, mockPrefixProvider);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender).sendMessageAsync(VALID_CHAT_ID, FINAL_MESSAGE);
        verify(mockSendResponse).isOk();
        verify(mockSendResponse).errorCode();
        verify(mockSendResponse).description();
    }

    @Test
    @DisplayName("Should log error when sending fails with an exception")
    void sendMessage_whenFutureCompletesExceptionally_shouldLogTheError() {
        // Arrange
        when(mockAppConfigManager.getLongSettingValue(Settings.TELEGRAM_CHAT_ID)).thenReturn(VALID_CHAT_ID);
        mockedFactory.when(() -> TelegramNotificationSenderFactory.create(mockAppConfigManager))
                .thenReturn(Optional.of(mockSender));

        IOException testException = new IOException("Network error");
        CompletableFuture<SendResponse> future = CompletableFuture.failedFuture(testException);
        when(mockSender.sendMessageAsync(VALID_CHAT_ID, FINAL_MESSAGE)).thenReturn(future);

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager, mockPrefixProvider);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender).sendMessageAsync(VALID_CHAT_ID, FINAL_MESSAGE);
        assertEquals(1, logger.getLoggingEvents().size());
        assertEquals(Level.ERROR, logger.getLoggingEvents().get(0).getLevel());
        assertEquals("Failed to send Telegram message.", logger.getLoggingEvents().get(0).getMessage());
        //noinspection OptionalGetWithoutIsPresent
        assertSame(testException, logger.getLoggingEvents().get(0).getThrowable().get());
    }

}