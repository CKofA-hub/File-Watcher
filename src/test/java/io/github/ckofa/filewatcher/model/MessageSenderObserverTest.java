package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageSenderObserverTest {

    @Mock
    private MessageManager messageManager;

    @Mock
    private CustomWatchEvent watchEvent;

    @Mock
    private WatchEvent<?> nestedWatchEvent;

    @TempDir
    private Path tempDir;

    private MessageSenderObserver messageSenderObserver;

    private final LocalDateTime eventTime = LocalDateTime.of(2025, 10, 27, 10, 30, 0);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String formattedTime = eventTime.format(formatter);

    @BeforeEach
    void setUp() {
        messageSenderObserver = new MessageSenderObserver(messageManager);
        when(watchEvent.getWatchPath()).thenReturn(tempDir);
        when(watchEvent.getEventTime()).thenReturn(eventTime);
        doReturn(nestedWatchEvent).when(watchEvent).getWatchEvent();
    }

    private void setupEvent(String name, WatchEvent.Kind<?> kind, boolean isDirectory) throws IOException {
        Path path = tempDir.resolve(name);
        if (isDirectory) {
            Files.createDirectory(path);
        } else {
            Files.createFile(path);
        }
        doReturn(Path.of(name)).when(nestedWatchEvent).context();
        doReturn(kind).when(nestedWatchEvent).kind();
    }

    @Test
    void onEvent_whenFileIsCreated_shouldSendCorrectMessage() throws IOException {
        // Arrange
        String fileName = "test.txt";
        setupEvent(fileName, StandardWatchEventKinds.ENTRY_CREATE, false);
        String expectedMessage = String.format("%s %s в папке %s %s. Время события: %s",
                "Файл",
                fileName,
                tempDir.toAbsolutePath(),
                "был создан",
                formattedTime);

        // Act
        messageSenderObserver.onEvent(watchEvent);

        // Assert
        verify(messageManager).sendMessage(expectedMessage);
    }

    @Test
    void onEvent_whenDirectoryIsCreated_shouldSendCorrectMessage() throws IOException {
        // Arrange
        String folderName = "testFolder";
        setupEvent(folderName, StandardWatchEventKinds.ENTRY_CREATE, true);
        String expectedMessage = String.format("%s %s в папке %s %s. Время события: %s",
                "Папка",
                folderName,
                tempDir.toAbsolutePath(),
                "была создана",
                formattedTime);

        // Act
        messageSenderObserver.onEvent(watchEvent);

        // Assert
        verify(messageManager).sendMessage(expectedMessage);
    }

    @Test
    void onEvent_whenFileIsModified_shouldSendCorrectMessage() throws IOException {
        // Arrange
        String fileName = "test.txt";
        setupEvent(fileName, StandardWatchEventKinds.ENTRY_MODIFY, false);
        String expectedMessage = String.format("%s %s в папке %s %s. Время события: %s",
                "Файл",
                fileName,
                tempDir.toAbsolutePath(),
                "был изменен",
                formattedTime);

        // Act
        messageSenderObserver.onEvent(watchEvent);

        // Assert
        verify(messageManager).sendMessage(expectedMessage);
    }

    @Test
    void onEvent_whenDirectoryIsModified_shouldSendCorrectMessage() throws IOException {
        // Arrange
        String folderName = "testFolder";
        setupEvent(folderName, StandardWatchEventKinds.ENTRY_MODIFY, true);
        String expectedMessage = String.format("%s %s в папке %s %s. Время события: %s",
                "Папка",
                folderName,
                tempDir.toAbsolutePath(),
                "была изменена",
                formattedTime);

        // Act
        messageSenderObserver.onEvent(watchEvent);

        // Assert
        verify(messageManager).sendMessage(expectedMessage);
    }

    @Test
    void onEvent_whenObjectIsDeleted_shouldSendCorrectMessage() throws IOException {
        // Arrange
        String deletedName = "deleted.txt";
        doReturn(Path.of(deletedName)).when(nestedWatchEvent).context();
        doReturn(StandardWatchEventKinds.ENTRY_DELETE).when(nestedWatchEvent).kind();

        String expectedMessage = String.format("%s %s в папке %s %s. Время события: %s",
                "Объект",
                deletedName,
                tempDir.toAbsolutePath(),
                "был удален",
                formattedTime);

        // Act
        messageSenderObserver.onEvent(watchEvent);

        // Assert
        verify(messageManager).sendMessage(expectedMessage);
    }

}