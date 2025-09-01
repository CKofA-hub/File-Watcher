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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderFactoryTest {

    @Mock
    private AppConfigManager mockConfig;

    @Test
    @DisplayName("Constructor should throw UnsupportedOperationException")
    void constructor_shouldThrowException() throws NoSuchMethodException {
        // Arrange
        Constructor<EmailNotificationSenderFactory> constructor = EmailNotificationSenderFactory.class.getDeclaredConstructor();
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

    @Test
    @DisplayName("Should return empty Optional when email sending is disabled")
    void create_whenEmailSendIsDisabled_shouldReturnEmpty() {
        // Arrange
        when(mockConfig.isSettingEnabled(Settings.MAIL_SEND_ENABLED)).thenReturn(false);

        // Act
        Optional<EmailNotificationSender> result = EmailNotificationSenderFactory.create(mockConfig);

        // Assert
        assertTrue(result.isEmpty(), "Result should be empty when email is disabled");
        verify(mockConfig, never()).getSettingValue(any(Settings.class));
    }

    @Test
    @DisplayName("Should create a corporate sender when corporate mode is enabled")
    void create_whenCorporateModeIsEnabled_shouldReturnSender() {
        // Arrange
        when(mockConfig.isSettingEnabled(Settings.MAIL_SEND_ENABLED)).thenReturn(true);
        when(mockConfig.isSettingEnabled(Settings.MAIL_CORPORATE_ENABLE)).thenReturn(true);

        when(mockConfig.getSettingValue(Settings.MAIL_SENDER_EMAIL)).thenReturn("user@corporate.com");
        when(mockConfig.getSettingValue(Settings.MAIL_SENDER_PASSWORD)).thenReturn("password");
        when(mockConfig.getSettingValue(Settings.MAIL_SMTP_HOST)).thenReturn("smtp.corporate.com");
        when(mockConfig.getIntSettingValue(Settings.MAIL_SMTP_PORT)).thenReturn(587);

        // Act
        Optional<EmailNotificationSender> result = EmailNotificationSenderFactory.create(mockConfig);

        // Assert
        assertTrue(result.isPresent(), "Sender should be created for valid corporate config");
    }

    @Test
    @DisplayName("Should create a public sender when corporate mode is disabled")
    void create_whenPublicModeIsEnabled_shouldReturnSender() {
        // Arrange
        when(mockConfig.isSettingEnabled(Settings.MAIL_SEND_ENABLED)).thenReturn(true);
        when(mockConfig.isSettingEnabled(Settings.MAIL_CORPORATE_ENABLE)).thenReturn(false);

        when(mockConfig.getSettingValue(Settings.MAIL_SENDER_EMAIL)).thenReturn("user@gmail.com");
        when(mockConfig.getSettingValue(Settings.MAIL_SENDER_PASSWORD)).thenReturn("password");
        when(mockConfig.getSettingValue(Settings.MAIL_SMTP_HOST)).thenReturn("smtp.gmail.com");
        when(mockConfig.getIntSettingValue(Settings.MAIL_SMTP_PORT)).thenReturn(465);

        // Act
        Optional<EmailNotificationSender> result = EmailNotificationSenderFactory.create(mockConfig);

        // Assert
        assertTrue(result.isPresent(), "Sender should be created for valid public config");
    }

    @Test
    @DisplayName("Should return empty Optional on configuration error")
    void create_whenConfigIsInvalid_shouldReturnEmpty() {
        // Arrange
        when(mockConfig.isSettingEnabled(Settings.MAIL_SEND_ENABLED)).thenReturn(true);
        when(mockConfig.getSettingValue(any())).thenReturn("some_value"); // email, pass, host

        when(mockConfig.getIntSettingValue(Settings.MAIL_SMTP_PORT)).thenThrow(new NumberFormatException("Invalid port"));

        // Act
        Optional<EmailNotificationSender> result = EmailNotificationSenderFactory.create(mockConfig);

        // Assert
        assertTrue(result.isEmpty(), "Result should be empty on configuration error");
    }

}