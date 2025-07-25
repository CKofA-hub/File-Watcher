package io.github.ckofa.filewatcher.model;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationSenderTest {

    private TelegramNotificationSender sender;

    @Nested
    @DisplayName("Testing class constructors")
    class ConstructorsTests {

        @Test
        void constructorWithoutProxy_whenTokenIsValid() {
            sender = new TelegramNotificationSender("validToken");
            assertNotNull(sender);
        }

        @Test
        void constructorWithoutProxy_whenTokenIsNullOrEmpty_shouldThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new TelegramNotificationSender(null));
            assertEquals(exception.getMessage(), "Token must not be null or empty");
            exception = assertThrows(IllegalArgumentException.class, () -> new TelegramNotificationSender(""));
            assertEquals(exception.getMessage(), "Token must not be null or empty");
        }

        @Test
        void constructorWithProxy_whenParametersValid() {
            sender = new TelegramNotificationSender("validToken", "proxyAddress", 8080);
            assertNotNull(sender);
        }

        @Test
        void constructorWithProxy_whenParametersInvalid_shouldThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new TelegramNotificationSender("validToken", "", 8080));
            assertEquals(exception.getMessage(), "Proxy address must not be null or empty");
            exception = assertThrows(IllegalArgumentException.class,
                    () -> new TelegramNotificationSender("validToken", "proxyAddress", 80000));
            assertEquals(exception.getMessage(), "Port must be between 1 and 65535");
        }

        @Test
        void constructorWithProxyAuth_whenParametersValid() {
            sender = new TelegramNotificationSender("validToken", "proxyAddress", 8080, "login", "password");
            assertNotNull(sender);
        }

        @Test
        void constructorWithProxyAuth_whenLoginAndPasswordNull_shouldThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new TelegramNotificationSender("validToken", "proxyAddress", 8080, null, null));
            assertEquals(exception.getMessage(), "Proxy login and password must not be null");
        }
    }

    @Nested
    @DisplayName("Testing methods for sending messages")
    class MethodTests {

        @Mock
        private TelegramBot bot;
        private final long chatId = 123456L;
        private final String message = "test message";

        @BeforeEach
        void setUp() throws NoSuchFieldException, IllegalAccessException {
            sender = new TelegramNotificationSender("validToken");

            injectMockTelegramBot(sender);
        }

        @Test
        void sendMessage_whenValidInput(){
            sender.sendMessage(chatId, message);

            //Check that bot.execute was called with the correct parameters
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot, times(1)).execute(captor.capture());
            SendMessage value = captor.getValue();
            assertAll(
                    () -> assertEquals(chatId, value.getParameters().get("chat_id")),
                    () -> assertEquals(message, value.getParameters().get("text"))
            );
        }

        @Test
        void sendMessage_whenMessageNullOrEmpty_shouldThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> sender.sendMessage(chatId, null));
            assertEquals(exception.getMessage(), "Message must not be null or empty");
            exception = assertThrows(IllegalArgumentException.class, () -> sender.sendMessage(chatId, ""));
            assertEquals(exception.getMessage(), "Message must not be null or empty");
        }

        @Test
        void sendMessageAsyncWithCallback_whenValidInput() {
            Callback<SendMessage, SendResponse> callback = mock(Callback.class);

            sender.sendMessageAsync(chatId, message, callback);

            //Check that bot.execute was called with the correct parameters
            ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot, times(1)).execute(messageCaptor.capture(), eq(callback));
            SendMessage value = messageCaptor.getValue();
            assertAll(
                    () -> assertEquals(chatId, value.getParameters().get("chat_id")),
                    () -> assertEquals(message, value.getParameters().get("text"))
            );
        }

        @Test
        void sendMessageAsyncWithCallback_whenNullCallback_shouldThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> sender.sendMessageAsync(chatId, message, null));
            assertEquals(exception.getMessage(), "Callback must not be null");
        }

        @Test
        void sendMessageAsyncFireAndForget_whenValidInput() throws NoSuchFieldException, IllegalAccessException {
            sender.sendMessageAsync(chatId, message);

            ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
            verify(bot, times(1)).execute(messageCaptor.capture(), any(Callback.class));
            SendMessage value = messageCaptor.getValue();
            assertAll(
                    () -> assertEquals(chatId, value.getParameters().get("chat_id")),
                    () -> assertEquals(message, value.getParameters().get("text"))
            );
        }

        @Test
        void sendMessageAsyncFireAndForget_whenMessageNullOrEmpty_shouldThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> sender.sendMessageAsync(chatId, null));
            assertEquals(exception.getMessage(), "Message must not be null or empty");
            exception = assertThrows(IllegalArgumentException.class, () -> sender.sendMessageAsync(chatId, ""));
            assertEquals(exception.getMessage(), "Message must not be null or empty");
        }

        private void injectMockTelegramBot(TelegramNotificationSender sender) throws NoSuchFieldException, IllegalAccessException {
            //Use reflection to replace the private bot field with mock
            Field botField = TelegramNotificationSender.class.getDeclaredField("bot");
            botField.setAccessible(true);
            botField.set(sender, bot);
        }
    }
}