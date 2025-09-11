package io.github.ckofa.filewatcher.model;

import org.slf4j.helpers.MessageFormatter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A simple implementation of {@link MessageFormatter} that creates a message prefix
 * containing the current timestamp and a static text identifier.
 * <p>
 * The default format is "dd-MM-yyyy HH:mm:ss : File Watcher : ".
 * This class is thread-safe.
 * </p>
 */
public class SimpleMessagePrefixProvider implements MessagePrefixProvider {

    private static final String DEFAULT_PREFIX_TEXT = "File Watcher";
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final String prefixText;
    private final DateTimeFormatter formatter;

    /**
     * Creates a formatter with the default prefix text ("File Watcher") and date-time format ("dd-MM-yyyy HH:mm:ss").
     */
    public SimpleMessagePrefixProvider() {
        this(DEFAULT_PREFIX_TEXT, DEFAULT_FORMATTER);
    }

    /**
     * Creates a formatter with custom prefix text and date-time formatter.
     *
     * @param prefixText the static text to include in the prefix.
     * @param formatter  the formatter for the timestamp.
     */
    public SimpleMessagePrefixProvider(String prefixText, DateTimeFormatter formatter) {
        this.prefixText = prefixText;
        this.formatter = formatter;
    }

    @Override
    public String getMessagePrefix() {
        String currentTime = LocalDateTime.now().format(formatter);
        return String.format("%s : %s : ", currentTime, prefixText);
    }
}
