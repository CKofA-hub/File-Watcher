package io.github.ckofa.filewatcher.model;

import org.jetbrains.annotations.NotNull;

import java.nio.file.*;
import java.time.LocalDateTime;

/**
 * Represents a file system watch event, encapsulating the original {@link WatchEvent}
 * with additional contextual information.
 * <p>
 * This record serves as an immutable data carrier, providing the path of the watched directory
 * and a precise timestamp for when the event was captured.
 *
 * @param watchEvent the original event from the {@link java.nio.file.WatchService}.
 * @param watchPath  the path to the directory where the event occurred.
 * @param eventTime  the timestamp indicating when this event object was created.
 */
public record CustomWatchEvent(WatchEvent<?> watchEvent, Path watchPath, LocalDateTime eventTime) {

    /**
     * Constructs a new {@code CustomWatchEvent} with the current system time.
     *
     * @param watchEvent the original event from the {@link java.nio.file.WatchService}.
     * @param watchPath  the path to the directory where the event occurred.
     */
    public CustomWatchEvent(WatchEvent<?> watchEvent, Path watchPath) {
        this(watchEvent, watchPath, LocalDateTime.now());
    }

    /**
     * Returns a formatted string representation of the event.
     *
     * @return a string detailing the event kind, context, path, and time.
     */
    @NotNull
    @Override
    public String toString() {
        return String.format("Event: %s, NameObject: %s, Path: %s, Event Time: %s",
                watchEvent.kind().name(),
                watchEvent.context().toString(),
                watchPath.toString(),
                eventTime);
    }
}
