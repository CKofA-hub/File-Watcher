package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageManagerTest {

    @Mock
    private MessageSender mockSender1;

    @Mock
    private MessageSender mockSender2;

    private final String TEST_MESSAGE = "Test message";

    @Test
    @DisplayName("Constructor should throw NullPointerException when given a null list")
    void constructor_whenGivenNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new MessageManager(null),
                "Constructor should fail fast if the senders list is null.");

    }

    @Test
    @DisplayName("Constructor should create a defensive copy of the sender list")
    void constructor_createsDefensiveCopyOfSenderList() {
        // Arrange: Create a mutable list of senders
        List<MessageSender> originalSenders = new ArrayList<>();
        originalSenders.add(mockSender1);

        MessageManager messageManager = new MessageManager(originalSenders);

        // Act: Modify the original list AFTER the manager has been created
        originalSenders.add(mockSender2);

        // Call the manager's method
        messageManager.sendMessage(TEST_MESSAGE);

        // Assert: Verify that the manager used the state of the list at the time of its creation,
        // not the modified state. This proves it's using an internal copy.
        verify(mockSender1, times(1)).sendMessage(TEST_MESSAGE);

        // Verify that the sender added later was NOT called
        verify(mockSender2, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("sendMessage should call sendMessage on all configured senders")
    void sendMessage_whenSendersExist_callsSendMessageOnAllSenders() {
        // Arrange
        List<MessageSender> senders = List.of(mockSender1, mockSender2);
        MessageManager messageManager = new MessageManager(senders);

        // Act
        messageManager.sendMessage(TEST_MESSAGE);

        // Assert
        verify(mockSender1, times(1)).sendMessage(TEST_MESSAGE);
        verify(mockSender2, times(1)).sendMessage(TEST_MESSAGE);
    }

    @Test
    @DisplayName("sendMessage should continue processing even if one sender fails")
    void sendMessage_whenOneSenderFails_continuesWithNextSender() {
        // Arrange
        doThrow(new RuntimeException("Simulated network error")).when(mockSender1).sendMessage(anyString());

        List<MessageSender> senders = List.of(mockSender1, mockSender2);
        MessageManager messageManager = new MessageManager(senders);

        // Act & Assert: The call to the manager's sendMessage should not throw an exception itself
        assertDoesNotThrow(() -> messageManager.sendMessage(TEST_MESSAGE),
                "MessageManager should handle exceptions from senders gracefully.");

        // Assert: Verify that sendMessage was attempted on both mocks
        verify(mockSender1, times(1)).sendMessage(TEST_MESSAGE);
        verify(mockSender2, times(1)).sendMessage(TEST_MESSAGE);
    }

    @Test
    @DisplayName("sendMessage should do nothing and not fail for an empty sender list")
    void sendMessage_whenSenderListIsEmpty_doesNotThrowException() {
        // Arrange
        MessageManager messageManager = new MessageManager(new ArrayList<>());

        // Act & Assert
        assertDoesNotThrow(() -> messageManager.sendMessage(TEST_MESSAGE),
                "sendMessage should handle an empty list without errors.");

        // Verify that no senders were ever called
        verifyNoInteractions(mockSender1, mockSender2);
    }

}