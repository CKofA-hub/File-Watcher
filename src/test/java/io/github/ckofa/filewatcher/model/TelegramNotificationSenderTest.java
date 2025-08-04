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
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationSenderTest {

    @Nested
    @DisplayName("Testing class constructors")
    class ConstructorsTests {

        @Test
        void createInstance_whenTokenIsValid_shouldReturnInstance() {
            TelegramNotificationSender sender = TelegramNotificationSender.create("validToken");
            assertNotNull(sender);
        }

        @Test
        void createInstance_whenTokenIsNullOrEmpty_shouldThrowException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> TelegramNotificationSender.create(null));
            assertEquals("Token must not be null or empty", exception.getMessage());
            exception = assertThrows(IllegalArgumentException.class, () -> TelegramNotificationSender.create(""));
            assertEquals("Token must not be null or empty", exception.getMessage());
        }

        @Test
        void createInstanceWithProxy_whenParametersValid_shouldReturnInstance() {
            TelegramNotificationSender sender = TelegramNotificationSender.createWithProxy("validToken", "proxyAddress", 8080);
            assertNotNull(sender);
        }

        @Test
        void createInstanceWithProxy_whenParametersInvalid_shouldThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> TelegramNotificationSender.createWithProxy("validToken", "", 8080));
            assertEquals("Proxy address must not be null or empty", exception.getMessage());
            exception = assertThrows(IllegalArgumentException.class,
                    () -> TelegramNotificationSender.createWithProxy("validToken", "proxyAddress", 80000));
            assertEquals("Port must be between 1 and 65535", exception.getMessage());
        }

        @Test
        void createInstanceWithProxyAuth_whenParametersValid_shouldReturnInstance() {
            TelegramNotificationSender sender = TelegramNotificationSender.createWithAuthenticatedProxy("validToken", "proxyAddress", 8080, "login", "password");
            assertNotNull(sender);
        }

        @Test
        void createInstanceWithProxyAuth_whenLoginAndPasswordNull_shouldThrowException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> TelegramNotificationSender.createWithAuthenticatedProxy("validToken", "proxyAddress", 8080, null, null));
            assertEquals("Proxy login and password must not be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Testing methods for sending messages")
    class MethodTests {

        @Mock
        private TelegramBot bot;
        @Captor
        private ArgumentCaptor<SendMessage> messageCaptor;
        @Captor
        private ArgumentCaptor<Callback<SendMessage, SendResponse>> callbackCaptor;

        private TelegramNotificationSender sender;
        private final long chatId = 123456L;
        private final String message = "test message";

        @BeforeEach
        void setUp() throws NoSuchFieldException, IllegalAccessException {
            sender = TelegramNotificationSender.create("validToken");

            injectMockTelegramBot(sender);
        }

        @Test
        void sendMessage_whenValidInput_shouldExecuteBot(){
            sender.sendMessage(chatId, message);

            //Check that bot.execute was called with the correct parameters
            verify(bot, times(1)).execute(messageCaptor.capture());
            SendMessage sentMessage = messageCaptor.getValue();

            assertAll("Verify message content",
                    () -> assertEquals(chatId, sentMessage.getParameters().get("chat_id")),
                    () -> assertEquals(message, sentMessage.getParameters().get("text"))
            );
        }

        @Test
        void sendMessage_whenMessageNullOrEmpty_shouldThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> sender.sendMessage(chatId, null));
            assertEquals("Message must not be null or empty", exception.getMessage());
            exception = assertThrows(IllegalArgumentException.class, () -> sender.sendMessage(chatId, ""));
            assertEquals("Message must not be null or empty", exception.getMessage());
        }

        @Test
        void sendMessageAsyncWithCallback_whenValidInput_shouldExecuteBot() {
            Callback<SendMessage, SendResponse> callback = mock(Callback.class);
            sender.sendMessageAsync(chatId, message, callback);

            //Check that bot.execute was called with the correct parameters
            verify(bot, times(1)).execute(messageCaptor.capture(), eq(callback));
            SendMessage sentMessage = messageCaptor.getValue();

            assertAll("Verify message content",
                    () -> assertEquals(chatId, sentMessage.getParameters().get("chat_id")),
                    () -> assertEquals(message, sentMessage.getParameters().get("text"))
            );
        }

        @Test
        void sendMessageAsyncWithCallback_whenNullCallback_shouldThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> sender.sendMessageAsync(chatId, message, null));
            assertEquals("Callback must not be null", exception.getMessage());
        }

        @Test
        void sendMessageAsync_CompletableFuture_onSuccess() {
            CompletableFuture<SendResponse> future = sender.sendMessageAsync(chatId, message);
            
            verify(bot, times(1)).execute(messageCaptor.capture(), callbackCaptor.capture());

            SendResponse mockResponse = mock(SendResponse.class);
            callbackCaptor.getValue().onResponse(messageCaptor.getValue(), mockResponse);

            assertTrue(future.isDone(), "Future should be done after onResponse is called");
            assertFalse(future.isCompletedExceptionally(), "Future should not be completed exceptionally on success");
            assertSame(mockResponse, future.join(), "The object from future should be the same instance as the mock response");

            SendMessage sentMessage = messageCaptor.getValue();
            assertAll("Verify message content",
                    () -> assertEquals(chatId, sentMessage.getParameters().get("chat_id")),
                    () -> assertEquals(message, sentMessage.getParameters().get("text"))
            );
        }

        @Test
        void sendMessageAsync_CompletableFuture_onFailure() {
            CompletableFuture<SendResponse> future = sender.sendMessageAsync(chatId, message);

            verify(bot, times(1)).execute(messageCaptor.capture(), callbackCaptor.capture());

            IOException exception = new IOException("Network error");
            callbackCaptor.getValue().onFailure(messageCaptor.getValue(), exception);

            assertTrue(future.isDone(), "Future should be done even if it failed");
            assertTrue(future.isCompletedExceptionally(), "Future should be completed exceptionally on failure");

            ExecutionException thrown = assertThrows(ExecutionException.class,
                    future::get,
                    "Getting result from a failed future should throw ExecutionException");
            assertSame(exception, thrown.getCause(), "The cause of the ExecutionException should be our original IOException");
        }

        private void injectMockTelegramBot(TelegramNotificationSender sender) throws NoSuchFieldException, IllegalAccessException {
            //Use reflection to replace the private bot field with mock
            Field botField = TelegramNotificationSender.class.getDeclaredField("bot");
            botField.setAccessible(true);
            botField.set(sender, bot);
        }
    }
}