package io.github.ckofa.filewatcher.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.time.format.DateTimeFormatter;

/**
 * An observer that constructs and sends a text message about a file system event.
 * It receives a {@link CustomWatchEvent} and uses a {@link MessageManager} to send the message.
 */
public class MessageSenderObserver implements WatchEventObserver {

    private final MessageManager messageManager;

    /**
     * Creates an observer.
     *
     * @param messageManager the manager used for sending messages.
     */
    public MessageSenderObserver(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    /**
     * Handles an incoming file system event.
     *
     * @param watchEvent the event containing information about the file change.
     */
    @Override
    public void onEvent(CustomWatchEvent watchEvent) {
        String msg = constructMessage(watchEvent);
        messageManager.sendMessage(msg);
    }

    /**
     * Constructs a text message based on the event data.
     *
     * @param watchEvent the event containing information about the file change.
     * @return a formatted string describing the event.
     */
    private String constructMessage(CustomWatchEvent watchEvent) {
        Path watchPath = watchEvent.getWatchPath();

        String objectName = watchEvent.getWatchEvent().context().toString();

        WatchEvent.Kind<?> eventKind = watchEvent.getWatchEvent().kind();

        String eventTypeObject;
        String wasVerb;
        String eventType;

        // For an ENTRY_DELETE event, the neutral term "Объект" (Object) is used because it's impossible
        // to reliably determine if the deleted path was a file or a directory after it's gone.
        // Calling Files.isDirectory(fullPath) on a deleted path always returns false, which would lead
        // to an incorrect message like "Файл ... был удален" (File ... was deleted) even for a directory.
        // This approach ensures the message is correct by sacrificing detail for deletion events.
        if (eventKind == StandardWatchEventKinds.ENTRY_DELETE) {
            eventTypeObject = "Объект";
            wasVerb = "был";
            eventType = "удален";
        } else {
            Path fullPath = watchPath.resolve(objectName);
            boolean isDirectory = Files.isDirectory(fullPath);

            eventTypeObject = isDirectory ? "Папка" : "Файл";
            wasVerb = isDirectory ? "была" : "был";

            if (eventKind == StandardWatchEventKinds.ENTRY_CREATE) {
                eventType = isDirectory ? "создана" : "создан";
            } else if (eventKind == StandardWatchEventKinds.ENTRY_MODIFY) {
                eventType = isDirectory ? "изменена" : "изменен";
            } else {
                eventType = "неизвестное событие";
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = watchEvent.getEventTime().format(formatter);

        return String.format("%s %s в папке %s %s %s. Время события: %s",
                eventTypeObject,
                objectName,
                watchPath.toAbsolutePath(),
                wasVerb,
                eventType,
                formattedTime);
    }
}
