package io.github.ckofa.filewatcher.model;

import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.ckofa.filewatcher.model.AppConfigManager.Settings;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
class TelegramSenderAdapterTest {

    private static final long VALID_CHAT_ID = 123456789L;
    private static final String TEST_MESSAGE = "Hello, World!";

    @Mock
    private AppConfigManager mockAppConfigManager;

    @Mock
    private TelegramNotificationSender mockSender;

    @Mock
    private SendResponse mockSendResponse;

    private MockedStatic<TelegramNotificationSenderFactory> mockedFactory;

    @BeforeEach
    void setUp() {
        mockedFactory = mockStatic(TelegramNotificationSenderFactory.class);
    }

    @AfterEach
    void tearDown() {
        mockedFactory.close();
    }

    @Test
    @DisplayName("Should successfully send the message if the configuration is correct")
    void sendMessage_whenConfigValid_shouldSuccess() {
        // Arrange
        when(mockAppConfigManager.getLongSettingsValue(Settings.TELEGRAM_CHAT_ID)).thenReturn(VALID_CHAT_ID);
        mockedFactory.when(() -> TelegramNotificationSenderFactory.create(mockAppConfigManager))
                .thenReturn(Optional.of(mockSender));

        CompletableFuture<SendResponse> future = CompletableFuture.completedFuture(mockSendResponse);
        when(mockSender.sendMessageAsync(VALID_CHAT_ID, TEST_MESSAGE)).thenReturn(future);
        when(mockSendResponse.isOk()).thenReturn(true);

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender).sendMessageAsync(VALID_CHAT_ID, TEST_MESSAGE);
        verify(mockSendResponse).isOk();
    }

    @Test
    @DisplayName("Should not send message when sender cannot be created")
    void sendMessage_whenSenderNotCreated_shouldNotSendMessage() {
        // Arrange
        when(mockAppConfigManager.getLongSettingsValue(Settings.TELEGRAM_CHAT_ID)).thenReturn(VALID_CHAT_ID);
        mockedFactory.when(() -> TelegramNotificationSenderFactory.create(mockAppConfigManager))
                .thenReturn(Optional.empty());

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender, never()).sendMessageAsync(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should not send message when chat ID is not configured")
    void sendMessage_whenChatIdIsMissing_shouldNotSendMessage() {
        // Arrange
        mockedFactory.when(() -> TelegramNotificationSenderFactory.create(mockAppConfigManager))
                .thenReturn(Optional.of(mockSender));
        when(mockAppConfigManager.getLongSettingsValue(Settings.TELEGRAM_CHAT_ID)).thenReturn(null);

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockAppConfigManager).getLongSettingsValue(Settings.TELEGRAM_CHAT_ID);
        verify(mockSender, never()).sendMessageAsync(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should log error when Telegram API returns an error response")
    void sendMessage_whenApiReturnsError_shouldHandleError() {
        // Arrange
        when(mockAppConfigManager.getLongSettingsValue(Settings.TELEGRAM_CHAT_ID)).thenReturn(VALID_CHAT_ID);
        mockedFactory.when(() -> TelegramNotificationSenderFactory.create(mockAppConfigManager))
                .thenReturn(Optional.of(mockSender));

        CompletableFuture<SendResponse> future = CompletableFuture.completedFuture(mockSendResponse);
        when(mockSender.sendMessageAsync(VALID_CHAT_ID, TEST_MESSAGE)).thenReturn(future);

        when(mockSendResponse.isOk()).thenReturn(false);
        when(mockSendResponse.errorCode()).thenReturn(400);
        when(mockSendResponse.description()).thenReturn("Bad Request: chat not found");

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender).sendMessageAsync(VALID_CHAT_ID, TEST_MESSAGE);
        verify(mockSendResponse).isOk();
        verify(mockSendResponse).errorCode();
        verify(mockSendResponse).description();
    }

    @Test
    @DisplayName("Should log error when sending fails with an exception")
    void sendMessage_whenFutureCompletesExceptionally_shouldHandleError() {
        // Arrange
        when(mockAppConfigManager.getLongSettingsValue(Settings.TELEGRAM_CHAT_ID)).thenReturn(VALID_CHAT_ID);
        mockedFactory.when(() -> TelegramNotificationSenderFactory.create(mockAppConfigManager))
                .thenReturn(Optional.of(mockSender));

        CompletableFuture<SendResponse> future = CompletableFuture.failedFuture(new IOException("Network error"));
        when(mockSender.sendMessageAsync(VALID_CHAT_ID, TEST_MESSAGE)).thenReturn(future);

        // Act
        TelegramSenderAdapter adapter = new TelegramSenderAdapter(mockAppConfigManager);
        adapter.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender).sendMessageAsync(VALID_CHAT_ID, TEST_MESSAGE);
    }

}