package io.github.ckofa.filewatcher.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CustomWatchEventTest {

    @Mock
    private WatchEvent<?> mockWatchEvent;

    private final Path watchPath = Paths.get("/path/watch"); // Use Unix-style path, Path will normalize it for the OS;

    @Test
    void twoArgumentConstructor_shouldSetCurrentTime() {
        // Arrange
        LocalDateTime beforeCreation = LocalDateTime.now();

        // Act
        CustomWatchEvent event = new CustomWatchEvent(mockWatchEvent, watchPath);

        // Assert
        assertNotNull(event.eventTime(), "Event time should not be null");
        // We verify that the event time is within a reasonable range of the creation time.
        assertTrue(event.eventTime().isAfter(beforeCreation.minusSeconds(1)), "Event time should be recent");
        assertTrue(event.eventTime().isBefore(LocalDateTime.now().plusSeconds(1)), "Event time should be recent");
    }

    @Test
    void toString_shouldReturnFormattedString() {
        // Arrange
        LocalDateTime fixedTime = LocalDateTime.of(2023, 10, 27, 10, 30, 0);

        doReturn(StandardWatchEventKinds.ENTRY_CREATE).when(mockWatchEvent).kind();
        doReturn(Paths.get("new_file.txt")).when(mockWatchEvent).context();

        CustomWatchEvent event = new CustomWatchEvent(mockWatchEvent, watchPath, fixedTime);

        // Act
        String result = event.toString();

        // Assert
        String expected = "Event: ENTRY_CREATE, NameObject: new_file.txt, Path: /path/watch, Event Time: 2023-10-27T10:30";
        // Normalize the actual result's path separators to Unix-style for platform-independent comparison
        assertEquals(expected, result.replace('\\', '/'), "toString() should produce a correctly formatted string.");
    }
}