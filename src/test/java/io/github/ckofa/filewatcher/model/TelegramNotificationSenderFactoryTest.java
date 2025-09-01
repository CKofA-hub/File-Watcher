package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static io.github.ckofa.filewatcher.model.AppConfigManager.Settings;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationSenderFactoryTest {

    @Mock
    private AppConfigManager mockConfig;

    @Test
    @DisplayName("create should return empty if telegram sending is disabled")
    void create_whenTelegramSendIsDisabled_shouldReturnEmpty() {
        // Arrange
        when(mockConfig.isSettingEnabled(Settings.TELEGRAM_SEND_ENABLED)).thenReturn(false);

        // Act
        Optional<TelegramNotificationSender> senderOptional = TelegramNotificationSenderFactory.create(mockConfig);

        // Assert
        assertTrue(senderOptional.isEmpty(), "Should return empty Optional when sending is disabled");
    }

    @Test
    @DisplayName("create should return empty if token is blank")
    void create_whenTokenIsMissing_shouldReturnEmpty() {
        // Arrange
        when(mockConfig.isSettingEnabled(Settings.TELEGRAM_SEND_ENABLED)).thenReturn(true);
        when(mockConfig.getSettingValue(Settings.TELEGRAM_BOT_TOKEN)).thenReturn("  "); // Blank token

        // Act
        Optional<TelegramNotificationSender> senderOptional = TelegramNotificationSenderFactory.create(mockConfig);

        // Assert
        assertTrue(senderOptional.isEmpty(), "Should return empty Optional when token is missing");
    }

    @Test
    @DisplayName("create should return a sender when only a valid token is provided")
    void create_withTokenAndNoProxy_shouldReturnSender() {
        // Arrange
        when(mockConfig.isSettingEnabled(Settings.TELEGRAM_SEND_ENABLED)).thenReturn(true);
        when(mockConfig.getSettingValue(Settings.TELEGRAM_BOT_TOKEN)).thenReturn("valid-token");
        when(mockConfig.isSettingEnabled(Settings.PROXY_ENABLED)).thenReturn(false);

        // Act
        Optional<TelegramNotificationSender> senderOptional = TelegramNotificationSenderFactory.create(mockConfig);

        // Assert
        assertTrue(senderOptional.isPresent(), "Should return a sender instance");
    }

    @Test
    @DisplayName("create should return a proxy sender when proxy is enabled without auth")
    void create_withProxyEnabled_butNoAuth_shouldReturnSender() {
        // Arrange
        when(mockConfig.isSettingEnabled(Settings.TELEGRAM_SEND_ENABLED)).thenReturn(true);
        when(mockConfig.getSettingValue(Settings.TELEGRAM_BOT_TOKEN)).thenReturn("valid-token");
        when(mockConfig.isSettingEnabled(Settings.PROXY_ENABLED)).thenReturn(true);
        when(mockConfig.getSettingValue(Settings.PROXY_HOST)).thenReturn("proxy.host.com");
        when(mockConfig.getIntSettingValue(Settings.PROXY_PORT)).thenReturn(8080);
        when(mockConfig.isSettingEnabled(Settings.PROXY_AUTH_ENABLED)).thenReturn(false);

        // Act
        Optional<TelegramNotificationSender> senderOptional = TelegramNotificationSenderFactory.create(mockConfig);

        // Assert
        assertTrue(senderOptional.isPresent(), "Should return a sender instance for proxy without auth");
    }

    @Test
    @DisplayName("create should return an authenticated proxy sender when proxy and auth are enabled")
    void create_withProxyAndAuthEnabled_shouldReturnSender() {
        // Arrange
        when(mockConfig.isSettingEnabled(Settings.TELEGRAM_SEND_ENABLED)).thenReturn(true);
        when(mockConfig.getSettingValue(Settings.TELEGRAM_BOT_TOKEN)).thenReturn("valid-token");
        when(mockConfig.isSettingEnabled(Settings.PROXY_ENABLED)).thenReturn(true);
        when(mockConfig.getSettingValue(Settings.PROXY_HOST)).thenReturn("proxy.host.com");
        when(mockConfig.getIntSettingValue(Settings.PROXY_PORT)).thenReturn(8080);
        when(mockConfig.isSettingEnabled(Settings.PROXY_AUTH_ENABLED)).thenReturn(true);
        when(mockConfig.getSettingValue(Settings.PROXY_USERNAME)).thenReturn("user");
        when(mockConfig.getSettingValue(Settings.PROXY_PASSWORD)).thenReturn("pass");

        // Act
        Optional<TelegramNotificationSender> senderOptional = TelegramNotificationSenderFactory.create(mockConfig);

        // Assert
        assertTrue(senderOptional.isPresent(), "Should return a sender instance for authenticated proxy");
    }

    @Test
    @DisplayName("Constructor should throw UnsupportedOperationException")
    void constructor_shouldThrowException() throws NoSuchMethodException {
        // Arrange
        Constructor<TelegramNotificationSenderFactory> constructor = TelegramNotificationSenderFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act & Assert
        InvocationTargetException thrown = assertThrows(
                InvocationTargetException.class,
                constructor::newInstance,
                "Constructor should throw an exception"
        );
        assertInstanceOf(
                UnsupportedOperationException.class,
                thrown.getCause(),
                "The cause should be UnsupportedOperationException"
        );
    }
}