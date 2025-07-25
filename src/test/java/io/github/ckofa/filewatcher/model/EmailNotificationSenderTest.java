package io.github.ckofa.filewatcher.model;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    private static final String SENDER_EMAIL = "sender@example.com";
    private static final String SENDER_PASSWORD = "password123";
    private static final String SMTP_HOST = "smtp.example.com";
    private static final int SMTP_PORT = 587;
    private static final String RECIPIENT_EMAIL = "recipient@example.com";
    private static final String TEXT_SUBJECT = "Test subject";
    private static final String TEXT_MESSAGE = "Test message";

    @Nested
    @DisplayName("Factory methods tests")
    class FactoryMethodsTests {

        @Test
        @DisplayName("createPublicEmailNotificationSender should succeed with valid credentials")
        void createPublicEmailNotificationSender_whenValidInput_Success() {
            assertDoesNotThrow(() -> EmailNotificationSender.createPublicEmailNotificationSender(
                    SENDER_EMAIL, SENDER_PASSWORD, SMTP_HOST, SMTP_PORT));
        }

        @Test
        @DisplayName("createCorporateEmailNotificationSender should succeed with valid credentials")
        void createCorporateEmailNotificationSender_whenValidInput_Success() {
            assertDoesNotThrow(() -> EmailNotificationSender.createCorporateEmailNotificationSender(
                    SENDER_EMAIL, SENDER_PASSWORD, SMTP_HOST, SMTP_PORT));
        }

        @ParameterizedTest(name = "Invalid email: \"{0}\"")
        @ValueSource(strings = {"invalid-email", "domain.com", " "})
        @NullAndEmptySource
        @DisplayName("Factory methods should throw IllegalArgumentException for invalid email")
        void createPublicEmailNotificationSender_whenInvalidEmail_shouldThrowsException(String invalidEmail) {
            String exceptionMsg = "Invalid sender email: " + invalidEmail;
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> EmailNotificationSender.createPublicEmailNotificationSender(
                            invalidEmail, SENDER_PASSWORD, SMTP_HOST, SMTP_PORT));
            assertEquals(exceptionMsg, exception.getMessage());
            exception = assertThrows(IllegalArgumentException.class,
                    () -> EmailNotificationSender.createCorporateEmailNotificationSender(
                            invalidEmail, SENDER_PASSWORD, SMTP_HOST, SMTP_PORT));
            assertEquals(exceptionMsg, exception.getMessage());
        }

        @ParameterizedTest(name = "Invalid password: \"{0}\"")
        @ValueSource(strings = {" ", "  "})
        @NullAndEmptySource
        @DisplayName("Factory methods should throw IllegalArgumentException for invalid password")
        void createPublicEmailNotificationSender_whenBlankPassword_shouldThrowsException(String invalidPassword) {
            String exceptionMsg = "Password must not be null or empty";
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> EmailNotificationSender.createPublicEmailNotificationSender(
                            SENDER_EMAIL, invalidPassword, SMTP_HOST, SMTP_PORT));
            assertEquals(exceptionMsg, exception.getMessage());
            exception = assertThrows(IllegalArgumentException.class,
                    () -> EmailNotificationSender.createCorporateEmailNotificationSender(
                            SENDER_EMAIL, invalidPassword, SMTP_HOST, SMTP_PORT));
            assertEquals(exceptionMsg, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Synchronous sendMessage tests")
    class SyncSendTests {

        private EmailNotificationSender emailSender;

        // MockedStatic is used for mocking static methods like Transport.send()
        private MockedStatic<Transport> mockedTransport;

        @BeforeEach
        void setUp() {
            emailSender = EmailNotificationSender.createPublicEmailNotificationSender(
                    SENDER_EMAIL, SENDER_PASSWORD, SMTP_HOST, SMTP_PORT);
            // Start mocking the static Transport class before each test
            mockedTransport = mockStatic(Transport.class);
        }

        @AfterEach
        void tearDown() {
            // Close the static mock after each test to avoid test pollution
            mockedTransport.close();
            emailSender.shutdown();
        }

        @Test
        @DisplayName("sendMessage should send email successfully")
        void sendMessage_whenValidInput_shouldSendEmail() throws MessagingException {
            // Arrange
            ArgumentCaptor<MimeMessage> mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

            // Act
            emailSender.sendMessage(TEXT_MESSAGE, TEXT_SUBJECT, RECIPIENT_EMAIL);

            // Assert
            mockedTransport.verify(() -> Transport.send(mimeMessageCaptor.capture()));
            MimeMessage sentMessage = mimeMessageCaptor.getValue();
            assertAll("Sent message properties",
                    () -> assertEquals(TEXT_SUBJECT, sentMessage.getSubject()),
                    () -> assertEquals(new InternetAddress(SENDER_EMAIL), sentMessage.getFrom()[0]),
                    () -> assertEquals(new InternetAddress(RECIPIENT_EMAIL), sentMessage.getRecipients(Message.RecipientType.TO)[0])
            );
        }

        @Test
        @DisplayName("sendMessage should throw MessagingException on transport failure")
        void sendMessage_whenTransportFailure_shouldThrowsException() {
            // Arrange
            mockedTransport.when(() -> Transport.send(any(MimeMessage.class)))
                    .thenThrow(new MessagingException("Connection failed"));

            // Act & Assert
            assertThrows(MessagingException.class,
                    () -> emailSender.sendMessage(TEXT_MESSAGE, TEXT_SUBJECT, RECIPIENT_EMAIL));
        }

        @ParameterizedTest(name = "Invalid content: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"\t", "  ", "\n"})
        @DisplayName("sendMessage should throw IllegalArgumentException for invalid message and subject")
        void sendMessage_whenInvalidContentOrSubject_shouldThrowException(String invalidInput) {
            String exMsg = "Message content must not be null or blank.";
            String exSub = "Message subject must not be null or blank.";
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> emailSender.sendMessage(invalidInput, TEXT_SUBJECT, RECIPIENT_EMAIL));
            assertEquals(exMsg, exception.getMessage());

            exception = assertThrows(IllegalArgumentException.class,
                    () -> emailSender.sendMessage(TEXT_MESSAGE, invalidInput, RECIPIENT_EMAIL));
            assertEquals(exSub, exception.getMessage());
        }

        @ParameterizedTest(name = "Invalid email: \"{0}\"")
        @ValueSource(strings = {"invalid-email", "domain.com", " "})
        @NullAndEmptySource
        @DisplayName("sendMessage should throw IllegalArgumentException for recipient email")
        void sendMessage_whenInvalidRecipientEmail_ThrowsException(String invalidEmail) {
            String exceptionMsg = "Invalid recipient email: " + invalidEmail;
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> emailSender.sendMessage(TEXT_MESSAGE, TEXT_SUBJECT, invalidEmail));
            assertEquals(exceptionMsg, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Asynchronous sendMessage tests")
    class AsyncSendTests {

        private EmailNotificationSender emailSender;

        @BeforeEach
        void setUp() {
            EmailNotificationSender realSender = EmailNotificationSender.createPublicEmailNotificationSender(
                    SENDER_EMAIL, SENDER_PASSWORD, SMTP_HOST, SMTP_PORT);
            emailSender = spy(realSender);
        }

        @AfterEach
        void tearDown() {
            emailSender.shutdown(); // Ensure the executor service is shut down
        }

        @Test
        @DisplayName("sendMessageAsync should complete successfully for valid input")
        void sendMessageAsync_whenValidInput_shouldCompleteSuccessfully() throws MessagingException {
            // Arrange
            doNothing().when(emailSender).sendMessage(anyString(), anyString(), anyString());

            // Act
            CompletableFuture<Void> future = emailSender.sendMessageAsync(TEXT_MESSAGE, TEXT_SUBJECT, RECIPIENT_EMAIL);

            // Assert
            //Wait for the future to complete without throwing exception
            assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
            // Verify that the async wrapper correctly called our stubbed sendMessage method
            verify(emailSender, times(1)).sendMessage(TEXT_MESSAGE, TEXT_SUBJECT, RECIPIENT_EMAIL);
        }

        @Test
        @DisplayName("sendMessageAsync should complete exceptionally on transport failure")
        void sendMessageAsync_whenTransportFailure_shouldCompleteExceptionally() throws MessagingException {
            // Arrange
            MessagingException mockException = new MessagingException("SMTP connection failed");
            doThrow(mockException).when(emailSender).sendMessage(anyString(), anyString(), anyString());

            // Act
            CompletableFuture<Void> future = emailSender.sendMessageAsync(TEXT_MESSAGE, TEXT_SUBJECT, RECIPIENT_EMAIL);

            // Assert
            // The future should complete with an exception
            ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            // Verify the cause of the exception to ensure our async logic works correctly
            Throwable cause = exception.getCause();
            String excMsgFromAsyncMethod = "Failed to send email: ";
            assertAll("Exception cause chain",
                    () -> assertInstanceOf(RuntimeException.class, cause),
                    () -> assertEquals(excMsgFromAsyncMethod + "SMTP connection failed", cause.getMessage()),
                    () -> assertSame(mockException, cause.getCause(), "The root cause should be our mocked exception")
            );
        }

        @Test
        @DisplayName("sendMessageAsync with callback should call onSuccess on success")
        void sendMessageAsyncWithCallback_whenValidInput_shouldCallOnSuccess(@Mock EmailNotificationSender.EmailCallback callback) throws MessagingException {
            // Arrange
            doNothing().when(emailSender).sendMessage(anyString(), anyString(), anyString());

            // Act
            CompletableFuture<Void> future = emailSender.sendMessageAsync(TEXT_MESSAGE, TEXT_SUBJECT, RECIPIENT_EMAIL, callback);

            // Assert
            assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
            verify(callback).onSuccess("Email sent successfully");
            verify(callback, never()).onFailure(anyString());
        }

        @Test
        @DisplayName("sendMessageAsync with callback should call onFailure on failure")
        void sendMessageWithCallback_whenTransportFailure_callsOnFailure(@Mock EmailNotificationSender.EmailCallback callback) throws MessagingException {
            // Arrange
            String errorMsg = "Authentification failed";
            doThrow(new MessagingException(errorMsg)).when(emailSender).sendMessage(anyString(), anyString(), anyString());

            // Act
            CompletableFuture<Void> future = emailSender.sendMessageAsync(TEXT_MESSAGE, TEXT_SUBJECT, RECIPIENT_EMAIL, callback);

            // Assert
            assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            verify(callback).onFailure("Failed to send email (async): " + errorMsg);
            verify(callback, never()).onSuccess(anyString());
        }

        @ParameterizedTest(name = "Test - [{index}] - {3}")
        @CsvSource(value = {
                "'' , 'Subject', 'r@a.com', 'Message content must not be null or blank.'",
                "' ' , 'Subject', 'r@a.com', 'Message content must not be null or blank.'",
                "null, 'Subject', 'r@a.com', 'Message content must not be null or blank.'",
                "'Message', '' , 'r@a.com', 'Message subject must not be null or blank.'",
                "'Message', ' ' , 'r@a.com', 'Message subject must not be null or blank.'",
                "'Message', null, 'r@a.com', 'Message subject must not be null or blank.'",
                "'Message', 'Subject', 'bad-email', 'Invalid recipient email: bad-email'"
        }, nullValues = {"null"})
        @DisplayName("sendMessageAsync should fail immediately for invalid parameters")
        void sendMessageAsync_whenInvalidParameters_shouldFailImmediately(
                String message, String subject, String recipient, String expectedError, @Mock EmailNotificationSender.EmailCallback callback) {
            // --- 1. Test the version WITHOUT a callback ---
            CompletableFuture<Void> futureNoCallback = emailSender.sendMessageAsync(message, subject, recipient);
            ExecutionException ex1 = assertThrows(ExecutionException.class, futureNoCallback::get, "Future without callback should fail");
            assertInstanceOf(IllegalArgumentException.class, ex1.getCause());
            assertEquals(expectedError, ex1.getCause().getMessage());

            // --- 2. Test the version WITH a callback ---
            CompletableFuture<Void> futureWithCallback = emailSender.sendMessageAsync(message, subject, recipient, callback);
            ExecutionException ex2 = assertThrows(ExecutionException.class, futureWithCallback::get, "Future with callback should fail");
            assertInstanceOf(IllegalArgumentException.class, ex2.getCause());
            verify(callback).onFailure("Invalid parameters: " + expectedError);
            verify(callback, never()).onSuccess(anyString());
        }
    }

    @Nested
    @DisplayName("Shutdown tests")
    class ShutdownTests {

        private EmailNotificationSender sender;
        private ExecutorService mockExecutor;

        @BeforeEach
        void setUp() throws NoSuchFieldException, IllegalAccessException {
            sender = EmailNotificationSender.createPublicEmailNotificationSender(
                    SENDER_EMAIL, SENDER_PASSWORD, SMTP_HOST, SMTP_PORT);
            mockExecutor = mock(ExecutorService.class);

            Field executorField = EmailNotificationSender.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            executorField.set(sender, mockExecutor);
        }

        @Test
        @DisplayName("shutdown should gracefully terminate the executor")
        void shutdown_whenTaskFinishInTime_shouldNotForceShutdown() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
            // Arrange
            // Simulate that termination was successful within the timeout
            when(mockExecutor.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(true);

            // Act
            sender.shutdown();

            // Assert
            InOrder inOrder = inOrder(mockExecutor);
            inOrder.verify(mockExecutor).shutdown();
            inOrder.verify(mockExecutor).awaitTermination(10, TimeUnit.SECONDS);
            verify(mockExecutor, never()).shutdownNow(); // Verify that forceful shutdown was NOT called
        }

        @Test
        @DisplayName("shutdown should forcefully terminate on timeout or interruption")
        void shutdown_whenTasksDoNotFinishInTime_shouldForceShutdown() throws Exception {
            // Arrange
            // Simulate that termination timed out
            when(mockExecutor.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(false);

            // Act
            sender.shutdown();

            // Assert
            InOrder inOrder = inOrder(mockExecutor);
            inOrder.verify(mockExecutor).shutdown();
            inOrder.verify(mockExecutor).awaitTermination(10, TimeUnit.SECONDS);
            inOrder.verify(mockExecutor).shutdownNow(); // Verify that forceful shutdown WAS called
        }

    }
}